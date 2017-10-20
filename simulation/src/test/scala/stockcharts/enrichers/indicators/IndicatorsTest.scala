package stockcharts.enrichers.indicators

import akka.stream.scaladsl.Source
import stockcharts.enrichers.StockchartsTest
import stockcharts.models.Price

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class IndicatorsTest extends StockchartsTest {
  import stockcharts.enrichers.PriceStreamsSupport._

  "Indicator calculations" should "work properly" in {
    val prices = (1 to 10).map(i => Price(today.plusDays(i), 0, 0, 0, close = i))
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
