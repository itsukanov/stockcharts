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

  def tick2Balance(initialBalance: Double) = {
    var latestBalance = initialBalance
    (ticks: TickIn) => ticks match {
      case TickIn(price, None) => latestBalance
      case TickIn(price, Some(_)) =>
        latestBalance = latestBalance - (price.close * lotSize)
        latestBalance
    }
  }

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

    val rightBalances = ticks.map(tick2Balance(initBalance.toDouble))
    calculatedBalances shouldBe rightBalances
  }

  it should "track equity with correlation with the price" in {
    def equity(currentPrice: Price, openOrders: List[Order]) = openOrders.map { order =>
      val diff = BigDecimal(order.openPrice.close) - BigDecimal(currentPrice.close)
      order.orderType match {
        case OrderType.Buy => BigDecimal(order.openPrice.close) + diff
        case OrderType.Sell => BigDecimal(order.openPrice.close) - diff
      }
    }.sum.toDouble

    def addOpenOrders(initialOpenOrders: List[Order]) = {
      var openOrders = initialOpenOrders
      val idGen = makeIdGen()

      (tickIn: TickIn) => tickIn match {
        case TickIn(price, None) => (price, openOrders)
        case TickIn(price, Some(TradeSignal.OpenBuy)) =>
          openOrders = openOrders :+ Order(idGen(), price, OrderType.Buy, lotSize)
          (price, openOrders)
        case TickIn(price, Some(TradeSignal.OpenSell)) =>
          openOrders = openOrders :+ Order(idGen(), price, OrderType.Sell, lotSize)
          (price, openOrders)
      }
    }

    val changesFromOpenOrders = ticks
      .map(addOpenOrders(List.empty[Order]))
      .map { case (price, openOrders) => equity(price, openOrders) }

    val balances = ticks.map(tick2Balance(initBalance.toDouble))
    val rightEquities = changesFromOpenOrders.zip(balances).map { case (c, b) => c + b }

    val calculatedEquities = Await.result(
      calculateAccountChanges(Source(ticks), accManagerFactory).toList, Duration.Inf)
      .map(_.account.equity)

    calculatedEquities shouldBe rightEquities
  }

}
