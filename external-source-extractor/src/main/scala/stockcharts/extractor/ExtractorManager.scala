package stockcharts.extractor

import akka.Done
import akka.actor.{Actor, ActorRef, Props, Stash, Terminated}
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Source}
import org.slf4j.LoggerFactory
import stockcharts.extractor.PricesExtractor.ExtractPricesIfNecessary
import stockcharts.extractor.quandl.QuandlClient
import stockcharts.json.JsonConverting
import stockcharts.kafka.{KafkaRecords, KafkaSink, KafkaUtils}
import stockcharts.models.{Stock, StockId}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait PricesState

object PricesState {
  case object UpToDate extends PricesState
  case object Stale extends PricesState
}

object PricesExtractorManager {
  def props(client: QuandlClient)(implicit materializer: ActorMaterializer) = Props(new PricesExtractorManager(client))
}

class PricesExtractorManager(client: QuandlClient)(implicit materializer: ActorMaterializer) extends Actor with Stash {

  val log = LoggerFactory.getLogger(this.getClass)
  var stock2Extractor = Map.empty[StockId, ActorRef]

  override def receive: Receive = {
    case msg@ExtractPricesIfNecessary(stock) => stock2Extractor.get(stock.stockId) match {
      case Some(extractor) => extractor forward msg
      case None =>
        val extractor = context.actorOf(PricesExtractor.props(client, stock), s"${stock.stockId.id}-prices-extractor")
        stock2Extractor = stock2Extractor + (stock.stockId -> extractor)

        log.debug(s"PricesExtractor for $stock has been created")
        context.watch(extractor)
        extractor forward msg
    }

    case Terminated(stopped) => stock2Extractor = stock2Extractor.filterNot {
      case (stockId, extractor) =>
        log.debug(s"PricesExtractor for $stockId has been stopped")
        stopped == extractor
    }
  }

}

object PricesExtractor {
  case class ExtractPricesIfNecessary(stock: Stock)

  def props(client: QuandlClient, stock: Stock)(implicit materializer: ActorMaterializer) = Props(new PricesExtractor(client, stock))
}

class PricesExtractor(client: QuandlClient, stock: Stock)(implicit materializer: ActorMaterializer) extends Actor with Stash {
  import context.dispatcher
  implicit val iSystem = context.system

  val log = LoggerFactory.getLogger(this.getClass)
  val timeout = 4 seconds

  def checkPricesState(): Future[PricesState] = KafkaUtils
    .isTopicEmpty(stock.topic, timeout)
    .map {
      case true => PricesState.Stale
      case false => PricesState.UpToDate
    }

  def extractPricesToKafka(): Future[Done] =
    for {
      prices <- client.getStockPrices(stock.stockId.id)
      kafka = KafkaSink()
      stream = Source(prices)
        .via(JsonConverting.toJsonFlow)
        .via(KafkaRecords.toKafkaRecordFlow(stock.topic))
        .toMat(kafka)(Keep.right)
      saving <- stream.run()
    } yield saving

  val upToDatePrices: Receive = {
    case ExtractPricesIfNecessary(_) => log.debug(s"Prices for $stock are already up-to-date. Prices extraction is not necessary")
  }

  val checkingPricesStateOrExtracting: Receive = {
    case PricesState.UpToDate =>
      log.debug(s"Prices for $stock are already up-to-date. Prices extraction is not necessary")
      unstashAll()
      context.become(upToDatePrices)

    case PricesState.Stale =>
      extractPricesToKafka() pipeTo self

    case akka.Done =>
      log.debug(s"Prices for $stock has been successfully extracted to kafka")
      unstashAll()
      context.become(upToDatePrices)

    case akka.actor.Status.Failure(thr) =>
      log.error(s"Error during $stock prices extraction", thr)
      unstashAll()
      context.become(readyToCheckPricesState)

    case any => stash()
  }

  lazy val readyToCheckPricesState: Receive = {
    case ExtractPricesIfNecessary(_) =>
      checkPricesState() pipeTo self
      context.become(checkingPricesStateOrExtracting)
  }

  override def receive: Receive = readyToCheckPricesState

}
