package stockcharts.simulation.enrichers.tradeevents

import java.time.LocalDate

import akka.stream.scaladsl.Source
import SimulationSupport._
import stockcharts.StockchartsTest
import stockcharts.simulation.enrichers.tradesignals.TradeSignal
import stockcharts.models.{Money, Price}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class TradeEventsTest extends StockchartsTest {

  val counter = Iterator.from(0)
  def price(close: Long) = {
    Price(today.plusDays(counter.next()), Money.zero, Money.zero, Money.zero, Money(close))
  }

  val initBalance = Money(100)
  val lotSize = 1
  val takeProfit = Money(10)
  val stopLoss = Money(5)
  val accManagerFactory = AccountManager(initBalance, constantSizeLotChooser(lotSize), takeProfitStopLossChecker(takeProfit, stopLoss))

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
      Source(ticks).via(calculateAccountChanges(accManagerFactory)).toList, Duration.Inf)
      .map(_.events)

    val orderIdGen = Iterator.from(1)
    val rightEvents: List[List[TradeEvent]] = ticks.map {
      case TickIn(price, None) => List.empty[TradeEvent]
      case TickIn(price, Some(TradeSignal.OpenBuy)) =>
        List(TradeEvent.OrderOpened(Order(orderIdGen.next(), price, OrderType.Buy, lotSize)))
      case TickIn(price, Some(TradeSignal.OpenSell)) =>
        List(TradeEvent.OrderOpened(Order(orderIdGen.next(), price, OrderType.Sell, lotSize)))
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
      Source(ticks).via(calculateAccountChanges(accManagerFactory)).toList, Duration.Inf)
      .map(_.account)

    calculatedAccounts shouldBe expectedAccounts
  }

  def makeOrderOpenedEventsGen = {
    val orderIdGen = Iterator.from(1)
    (price: Price, signal: Option[TradeSignal], canAfford: Boolean) => {
      signal match {
        case Some(TradeSignal.OpenBuy) if canAfford =>
          List(TradeEvent.OrderOpened(Order(orderIdGen.next(), price, OrderType.Buy, lotSize)))
        case Some(TradeSignal.OpenSell) if canAfford =>
          List(TradeEvent.OrderOpened(Order(orderIdGen.next(), price, OrderType.Sell, lotSize)))
        case _ => Nil
      }
    }
  }

  it should "be impossible to make the balance negative" in {
    val (ticks, expectedAccounts, canAffords) = List(
      (TickIn(price(50), None),                      Account(Money(100), Money(100)), true),
      (TickIn(price(50), Some(TradeSignal.OpenBuy)), Account(Money(50), Money(100)), true),
      (TickIn(price(51), Some(TradeSignal.OpenBuy)), Account(Money(50), Money(101)), false), // not enough balance to open
      (TickIn(price(50), Some(TradeSignal.OpenBuy)), Account(Money(0), Money(100)), true)
    ).unzip3

    val calculatedTicksOut = Await.result(
      Source(ticks).via(calculateAccountChanges(accManagerFactory)).toList, Duration.Inf)

    calculatedTicksOut.map(_.account) shouldBe expectedAccounts

    val eventGen = makeOrderOpenedEventsGen
    val expectedEvents = ticks zip canAffords map { case (TickIn(price, signal), canAfford) =>
      eventGen(price, signal, canAfford)
    }
    calculatedTicksOut.map(_.events) shouldBe expectedEvents
  }

  it should "close orders in stop loss and take profit cases" in {
    val takeProfit = 10
    val stopLoss = 5
    val accManagerFactory = AccountManager(initBalance, constantSizeLotChooser(lotSize), takeProfitStopLossChecker(Money(takeProfit), Money(stopLoss)))

    def price(date: String, close: Long) = Price(LocalDate.parse(date), Money.zero, Money.zero, Money.zero, Money(close))

    def createOrder(id: Long, price: Price, orderType: OrderType) = Order(id, price, orderType, lotSize)
    def buyOrderOpened(id: Long, openPrice: Price) = TradeEvent.OrderOpened(createOrder(id, openPrice, OrderType.Buy))
    def sellOrderOpened(id: Long, openPrice: Price) = TradeEvent.OrderOpened(createOrder(id, openPrice, OrderType.Sell))
    def buyOrderClosed(id: Long, openPrice: Price, closePrice: Price, balanceChange: Money) =
      TradeEvent.OrderClosed(createOrder(id, openPrice, OrderType.Buy), closePrice, balanceChange)
    def sellOrderClosed(id: Long, openPrice: Price, closePrice: Price, balanceChange: Money) =
      TradeEvent.OrderClosed(createOrder(id, openPrice, OrderType.Sell), closePrice, balanceChange)

    val (ticks, expectedAccounts, expectedEvents) = List( // todo simplify expectedEvents checking
      (TickIn(price("2012-05-01", 10), Some(TradeSignal.OpenBuy)),  Account(Money(90),  Money(100)), List(buyOrderOpened(1L, price("2012-05-01", 10)))),
      (TickIn(price("2012-05-02", 10 + takeProfit), None),          Account(Money(110), Money(110)), List(buyOrderClosed(1L, price("2012-05-01", 10), price("2012-05-02", 10 + takeProfit), Money(takeProfit)))),
      (TickIn(price("2012-05-03", 20), Some(TradeSignal.OpenBuy)),  Account(Money(90),  Money(110)), List(buyOrderOpened(2L, price("2012-05-03", 20)))),
      (TickIn(price("2012-05-04", 20 - stopLoss), None),            Account(Money(105), Money(105)), List(buyOrderClosed(2L, price("2012-05-03", 20), price("2012-05-04", 20 - stopLoss), Money(-stopLoss)))),
      (TickIn(price("2012-05-05", 15), Some(TradeSignal.OpenSell)), Account(Money(90),  Money(105)), List(sellOrderOpened(3L, price("2012-05-05", 15)))),
      (TickIn(price("2012-05-06", 15 - takeProfit), None),          Account(Money(115), Money(115)), List(sellOrderClosed(3L, price("2012-05-05", 15), price("2012-05-06", 15 - takeProfit), Money(takeProfit)))),
      (TickIn(price("2012-05-07", 5), Some(TradeSignal.OpenSell)),  Account(Money(110), Money(115)), List(sellOrderOpened(4L, price("2012-05-07", 5)))),
      (TickIn(price("2012-05-08", 5 + stopLoss), None),             Account(Money(110), Money(110)), List(sellOrderClosed(4L, price("2012-05-07", 5), price("2012-05-08", 5 + stopLoss), Money(-stopLoss))))
    ).unzip3

    val calculatedTicksOut = Await.result(
      Source(ticks).via(calculateAccountChanges(accManagerFactory)).toList, Duration.Inf)
    calculatedTicksOut.map(_.account) zip expectedAccounts foreach { case (calculated, expected) =>
        calculated shouldBe expected
    }

    val calculatedEvents = calculatedTicksOut.map(_.events)
    calculatedEvents zip expectedEvents foreach { case (calculated, expected) =>
      calculated shouldBe expected
    }
  }

}
