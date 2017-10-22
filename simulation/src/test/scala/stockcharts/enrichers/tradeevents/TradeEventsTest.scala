package stockcharts.enrichers.tradeevents

import akka.stream.scaladsl.Source
import stockcharts.enrichers.StockchartsTest
import stockcharts.enrichers.tradesignals.SimulationSupport._
import stockcharts.enrichers.tradesignals.TradeSignal
import stockcharts.models.{Money, Price}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class TradeEventsTest extends StockchartsTest {

  var count = 0
  def price(close: Long) = {
    count += 1
    Price(today.plusDays(count), Money.zero, Money.zero, Money.zero, Money(close))
  }

  val initBalance = Money(100)
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
    val rightEvents: List[List[TradeEvent]] = ticks.map {
      case TickIn(price, None) => List.empty[TradeEvent]
      case TickIn(price, Some(TradeSignal.OpenBuy)) =>
        List(TradeEvent.OrderOpened(Order(orderIdGen(), price, OrderType.Buy, lotSize)))
      case TickIn(price, Some(TradeSignal.OpenSell)) =>
        List(TradeEvent.OrderOpened(Order(orderIdGen(), price, OrderType.Sell, lotSize)))
    }

    calculatedEvents shouldBe rightEvents
  }

  it should "decreases balance and tracks equity properly" in {
    val (ticks, expectedAccounts) = List(
      (TickIn(price(5), None),                       Account(Money(100), Money(100))),
      (TickIn(price(5), Some(TradeSignal.OpenBuy)),  Account(Money(95), Money(100))),
      (TickIn(price(10), None),                      Account(Money(95), Money(105))),
      (TickIn(price(8), None),                       Account(Money(95), Money(103))),
      (TickIn(price(8), Some(TradeSignal.OpenSell)), Account(Money(87), Money(103))),
      (TickIn(price(5), None),                       Account(Money(87), Money(103))),
      (TickIn(price(4), Some(TradeSignal.OpenSell)), Account(Money(83), Money(103))),
      (TickIn(price(3), None),                       Account(Money(83), Money(104)))
    ).unzip

    val calculatedAccounts = Await.result(
      calculateAccountChanges(Source(ticks), accManagerFactory).toList, Duration.Inf)
      .map(_.account)

    calculatedAccounts shouldBe expectedAccounts
  }

    it should "be impossible to make the balance negative" in {
      val (ticks, expectedAccounts, expectedEventsSize) = List(
        (TickIn(price(50), None),                       Account(Money(100), Money(100)), 0),
        (TickIn(price(50), Some(TradeSignal.OpenBuy)),  Account(Money(50), Money(100)), 1),
        (TickIn(price(51), Some(TradeSignal.OpenBuy)),  Account(Money(50), Money(101)), 0), // not enough balance to open
        (TickIn(price(50), Some(TradeSignal.OpenBuy)),  Account(Money(0),  Money(100)), 1)
      ).unzip3

      val calculatedTicksOut = Await.result(
        calculateAccountChanges(Source(ticks), accManagerFactory).toList, Duration.Inf)

      calculatedTicksOut.map(_.account) shouldBe expectedAccounts
      calculatedTicksOut.map(_.events.size) shouldBe expectedEventsSize
  }

}
