package stockcharts.simulation.enrichers.tradeevents

import akka.stream.scaladsl.Source
import stockcharts.simulation.enrichers.StockchartsTest
import stockcharts.simulation.enrichers.tradesignals.SimulationSupport._
import stockcharts.simulation.enrichers.tradesignals.TradeSignal
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
      calculateAccountChanges(Source(ticks), accManagerFactory).toList, Duration.Inf)
      .map(_.events)

    val orderIdGen = makeIdGen
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

  def makeOrderOpenedEventsGen = {
    val idGen = makeIdGen
    (price: Price, signal: Option[TradeSignal], canAfford: Boolean) => {
      signal match {
        case Some(TradeSignal.OpenBuy) if canAfford =>
          List(TradeEvent.OrderOpened(Order(idGen(), price, OrderType.Buy, lotSize)))
        case Some(TradeSignal.OpenSell) if canAfford =>
          List(TradeEvent.OrderOpened(Order(idGen(), price, OrderType.Sell, lotSize)))
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
      calculateAccountChanges(Source(ticks), accManagerFactory).toList, Duration.Inf)

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

    val (ticks, expectedAccounts) = List(
      (TickIn(price(10), Some(TradeSignal.OpenBuy)),  Account(Money(90),  Money(100))),
      (TickIn(price(10 + takeProfit), None),          Account(Money(110), Money(110))),
      (TickIn(price(20), Some(TradeSignal.OpenBuy)),  Account(Money(90),  Money(110))),
      (TickIn(price(20 - stopLoss), None),            Account(Money(105), Money(105))),
      (TickIn(price(15), Some(TradeSignal.OpenSell)), Account(Money(90),  Money(105))),
      (TickIn(price(15 - takeProfit), None),          Account(Money(115), Money(115))),
      (TickIn(price(5), Some(TradeSignal.OpenSell)),  Account(Money(110), Money(115))),
      (TickIn(price(5 + stopLoss), None),             Account(Money(110), Money(110)))
    ).unzip

    val calculatedTicksOut = Await.result(
      calculateAccountChanges(Source(ticks), accManagerFactory).toList, Duration.Inf)

    calculatedTicksOut.map(_.account) zip expectedAccounts foreach { case (calculated, expected) =>
        calculated shouldBe expected
    }

    def addOrderClosedEvents(openEvents: List[List[TradeEvent.OrderOpened]]) = {
      def impl(acc: List[List[TradeEvent]], rest: List[List[TradeEvent.OrderOpened]]): List[List[TradeEvent]] =
        rest match {
          case Nil => acc
          case x :: xs => x match {
            case Nil => impl(acc, xs)
            case event@List(TradeEvent.OrderOpened(order)) =>
              impl(acc :+ event :+ List(TradeEvent.OrderClosed(order)), xs)
          }
        }

      impl(Nil, openEvents)
    }

    val eventGen = makeOrderOpenedEventsGen
    val expectedOrderOpenedEvents = ticks
      .map(tick => eventGen(tick.price, tick.tradeSignal, true))
    val expectedEvents = addOrderClosedEvents(expectedOrderOpenedEvents)
    val calculatedEvents = calculatedTicksOut.map(_.events)

    calculatedEvents zip expectedEvents foreach { case (calculated, expected) =>
      calculated shouldBe expected
    }
  }

}
