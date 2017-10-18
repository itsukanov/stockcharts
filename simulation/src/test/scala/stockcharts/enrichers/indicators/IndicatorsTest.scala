package stockcharts.enrichers.indicators

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import stockcharts.models.Price
import stockcharts.enrichers.indicators.PriceStreamsSupport._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class IndicatorsTest extends FlatSpec with Matchers with BeforeAndAfterAll {

  implicit val as = ActorSystem("as-for-tests")
  implicit val materializer = ActorMaterializer()

  override def afterAll() = {
    as.terminate()
    materializer.shutdown()
  }

  val startTime = LocalDate.now()

  "Indicator calculations" should "work properly" in {
    val prices = (1 to 10).map(i => Price(startTime.plusDays(i), 0, 0, 0, close = i))
    val smaPeriod = 2

    val closePrices = prices.map(_.close)
    val averageClosePrices =
      closePrices.head +: (closePrices zip closePrices.tail).map { case (l, r) => (l + r) / smaPeriod }

    val smaCalculating = Source(prices)
      .calculate(SMAIndicator(smaPeriod))
      .runFold(List.empty[Double]) { case (list, v) => list :+ v }

    val sma = Await.result(smaCalculating, 3 seconds)

    sma should contain theSameElementsAs averageClosePrices
  }


}
