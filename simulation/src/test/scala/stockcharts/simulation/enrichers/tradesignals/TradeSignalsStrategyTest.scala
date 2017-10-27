package stockcharts.simulation.enrichers.tradesignals

import akka.stream.scaladsl.Source
import stockcharts.simulation.enrichers.StockchartsTest

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TradeSignalsStrategyTest extends StockchartsTest {

  "OverBoughtSoldStrategy" should "give right trade signals" in {
    import TradeSignalsSupport._

    val overBoughtLevel = 7
    val overSoldLevel = 3
    val (values, rightSignals) = List(
      (4, None),
      (3, None),
      (2, None),
      (5, Some(TradeSignal.OpenBuy)),
      (7, None),
      (9, None),
      (7, Some(TradeSignal.OpenSell))
    ).unzip

    val signalsCalculating = Source(values)
      .via(calculateTradeSignals(OverBoughtSoldStrategy(overBoughtLevel, overSoldLevel)))
      .toList

    val signals = Await.result(signalsCalculating, 3 seconds)
    signals should contain theSameElementsAs rightSignals
  }

}
