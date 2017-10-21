package stockcharts.enrichers.tradeevents

import akka.stream.scaladsl.Source
import stockcharts.enrichers.StockchartsTest
import stockcharts.enrichers.tradesignals.SimulationSupport._
import stockcharts.enrichers.tradesignals.TradeSignal
import stockcharts.models.Price

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class TradeEventsTest extends StockchartsTest {

  var count = 0
  def price(close: Double) = {
    count += 1
    Price(today.plusDays(count), 0, 0, 0, close)
  }

  val initBalance = 100
  val lotSize = 1
  val accManagerFactory = AccountManager(initBalance, constantSizeLotChooser(lotSize))

  "AccountManager" should "open positions according to trade signals" in {
    val ticks = List(
      TickIn(price(2), None),
      TickIn(price(2), Some(TradeSignal.OpenBuy)),
      TickIn(price(2), None),
      TickIn(price(2), Some(TradeSignal.OpenSell)),
      TickIn(price(2), None),
      TickIn(price(2), None)
    )

    val calculatedEvents = Await.result(
      calculateAccountChanges(Source(ticks), accManagerFactory).toList, Duration.Inf)
      .map(_.events)

    val orderIdGen = makeIdGen()
    val rightEvents: List[Set[TradeEvent]] = ticks.map {
      case TickIn(price, None) => Set.empty[TradeEvent]
      case TickIn(price, Some(TradeSignal.OpenBuy)) =>
        Set(TradeEvent.OrderOpened(Order(orderIdGen(), price, OrderType.Buy, lotSize))): Set[TradeEvent]
      case TickIn(price, Some(TradeSignal.OpenSell)) =>
        Set(TradeEvent.OrderOpened(Order(orderIdGen(), price, OrderType.Sell, lotSize))): Set[TradeEvent]
    }

    calculatedEvents shouldBe rightEvents
  }

  it should "decreases balance and tracks equity properly" in {
    val (ticks, expectedAccounts) = List(
      (TickIn(price(5), None),                       Account(100, 100)),
      (TickIn(price(5), Some(TradeSignal.OpenBuy)),  Account(95, 100)),
      (TickIn(price(10), None),                      Account(95, 105)),
      (TickIn(price(8), None),                       Account(95, 103)),
      (TickIn(price(8), Some(TradeSignal.OpenSell)), Account(87, 103)),
      (TickIn(price(5), None),                       Account(87, 103)),
      (TickIn(price(4), Some(TradeSignal.OpenSell)), Account(83, 103)),
      (TickIn(price(3), None),                       Account(83, 104))
    ).unzip

    val calculatedAccounts = Await.result(
      calculateAccountChanges(Source(ticks), accManagerFactory).toList, Duration.Inf)
      .map(_.account)

    calculatedAccounts shouldBe expectedAccounts
  }

}
