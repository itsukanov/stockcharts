package stockcharts.extractor

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, Props, Stash, Terminated}
import org.slf4j.LoggerFactory
import stockcharts.extractor.Extractor.ExtractPricesIfNecessary
import stockcharts.extractor.quandl.QuandlClient
import akka.pattern.pipe

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class StockId(id: String) extends AnyVal
case class Stock(id: StockId, name: String)

object Extractor {
  case class ExtractPricesIfNecessary(stock: Stock)
}

trait PricesState

object PricesState {
  case object UpToDate extends PricesState
  case object Stale extends PricesState
}


class PricesExtractorManager(client: QuandlClient) extends Actor with Stash {

  val log = LoggerFactory.getLogger(this.getClass)

  var stock2Extractor = Map.empty[StockId, ActorRef]

  override def receive: Receive = {
    case msg@ExtractPricesIfNecessary(stock) => stock2Extractor.get(stock.id) match {
      case Some(extractor) => extractor forward msg
      case None =>
        val extractor = context.actorOf(Props(classOf[PricesExtractor], client, stock), s"${stock.id.id}-prices-extractor")
        stock2Extractor = stock2Extractor + (stock.id -> extractor)

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


class PricesExtractor(client: QuandlClient, stock: Stock) extends Actor with Stash {
  import context.dispatcher

  val log = LoggerFactory.getLogger(this.getClass)

  def checkPricesState(): Future[PricesState] = Future.successful(PricesState.Stale)
  def extractPricesToKafka(): Future[Unit] = {
    val fut = client.getStockData("FB")
    val res = Await.result(fut, Duration.Inf)
    log.info(res)

    Future.successful() // todo check both
    Future.failed(new RuntimeException("cause Bobbie"))
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
      log.debug(s"Successfully extracted prices for $stock to kafka")
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
