package stockcharts.extractor.quandl

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import stockcharts.models.Price

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.postfixOps
import scala.util.Failure

private [quandl] case class GetPrices(stock: String, response: Promise[List[Price]])

class ThrottledQuandlClient(baseUrl: String,
                            apiKey: String,
                            delayBtwCalls: FiniteDuration,
                            system: ActorSystem)
                           (implicit val ec: ExecutionContext,
                            val m: ActorMaterializer) {

  val simpleClient = new SimpleQuandlClient(baseUrl, apiKey, system)

  val bufferSize = 100
  val overflowStrategy = akka.stream.OverflowStrategy.backpressure
  val quandlRequests = Source
    .queue[GetPrices](bufferSize, overflowStrategy)
    .throttle(1, delayBtwCalls, 1, ThrottleMode.Shaping)
    .to(Sink.foreach {
      case GetPrices(stock, response) => response.completeWith(simpleClient.getStockPrices(stock))
    }).run()

  def getStockPrices(stock: String): Future[List[Price]] = {
    val response = Promise[List[Price]]()

    val offering = quandlRequests.offer(GetPrices(stock, response))
    offering.onComplete {
      case Failure(thr) => response.failure(thr)
      case _ =>
    }

    response.future
  }

}

