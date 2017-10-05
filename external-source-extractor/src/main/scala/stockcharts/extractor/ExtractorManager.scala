package stockcharts.extractor

import akka.actor.{Actor, ActorRef, Props, Stash, Terminated}
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import org.slf4j.LoggerFactory
import stockcharts.KafkaUtils
import stockcharts.extractor.Extractor.ExtractPricesIfNecessary
import stockcharts.extractor.quandl.QuandlClient
import stockcharts.models.{Stock, StockId}

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

object Extractor {
  case class ExtractPricesIfNecessary(stock: Stock)
}

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

  def extractPricesToKafka(): Future[Unit] = {
    val fut = client.getStockData("FB")
    val res = Await.result(fut, Duration.Inf)
    log.info(res)

    Future.successful()
  }

  val upToDatePrices: Receive = {
    case ExtractPricesIfNecessary(_) => log.debug("Prices are up-to-date, prices extraction is not necessary")
  }

  val checkingPricesStateOrExtracting: Receive = {
    case PricesState.UpToDate =>
      log.debug(s"Prices for $stock are already up-to-date")
      unstashAll()
      context.become(upToDatePrices)

    case PricesState.Stale =>
      extractPricesToKafka()
        .map(_ => akka.Done) pipeTo self

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
