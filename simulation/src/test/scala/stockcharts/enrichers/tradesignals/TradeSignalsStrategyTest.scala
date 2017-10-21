package stockcharts.enrichers.tradesignals

import akka.stream.scaladsl.Source
import stockcharts.enrichers.{NumericStreamsSupport, StockchartsTest}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class TradeSignalsStrategyTest extends StockchartsTest {

  "OverBoughtSoldStrategy" should "give right trade signals" in {
    import NumericStreamsSupport._

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
      .calculateTradeSignals(OverBoughtSoldStrategy(overBoughtLevel, overSoldLevel))
      .toList

    val signals = Await.result(signalsCalculating, 3 seconds)
    signals should contain theSameElementsAs rightSignals
  }

}
