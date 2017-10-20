package stockcharts.enrichers.tradeevents

import akka.stream.scaladsl.Source
import stockcharts.enrichers.StockchartsTest
import stockcharts.enrichers.tradesignals.SimulationSupport._
import stockcharts.enrichers.tradesignals.TradeSignal
import stockcharts.models.Price

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class TradeEventsTest extends StockchartsTest {

  def priceGen = {
    var count = 0
    () => {
      count += 1
      Price(today.plusDays(count), 0, 0, 0, 2)
    }
  }

  val ticks = List(
    TickIn(priceGen(), None),
    TickIn(priceGen(), Some(TradeSignal.OpenBuy)),
    TickIn(priceGen(), None),
    TickIn(priceGen(), Some(TradeSignal.OpenSell)),
    TickIn(priceGen(), None),
    TickIn(priceGen(), None)
  )

  val initBalance = 100
  val lotSize = 1
  val accManagerFactory = AccountManager(initBalance, constantSizeLotChooser(lotSize))

  "AccountManager" should "open positions according to trade signals" in {
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

  it should "decrease balance after opening orders" in {
    val calculatedBalances = Await.result(
      calculateAccountChanges(Source(ticks), accManagerFactory).toList, Duration.Inf)
      .map(_.account.balance)

    var latestBalance = initBalance.toDouble
    val rightBalances = ticks.map {
      case TickIn(price, None) => latestBalance
      case TickIn(price, Some(_)) =>
        latestBalance = latestBalance - (price.close * lotSize)
        latestBalance
    }

    calculatedBalances shouldBe rightBalances
  }

}
