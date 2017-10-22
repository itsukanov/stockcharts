package stockcharts.enrichers.tradeevents

import akka.actor.{Actor, Props}
import stockcharts.enrichers.tradesignals.TradeSignal
import stockcharts.models.{Money, Price}

trait AccountManagerFactory {
  def props: Props
}

object AccountManager {
  def apply(initialBalance: Money,
            lotSizeChooser: (Price, TradeSignal) => Int) = new AccountManagerFactory {

    override def props: Props = Props(new AccountManager(initialBalance, lotSizeChooser))
  }
}

class AccountManager(initialBalance: Money,
                     lotSizeChooser: (Price, TradeSignal) => Int) extends Actor {

  var lastId = 0L
  def orderId = {
    lastId += 1
    lastId
  }

  def openNewOrders(tickIn: TickIn) = tickIn.tradeSignal match {
    case None => List.empty[Order]
    case Some(TradeSignal.OpenBuy) =>
      List(Order(orderId, tickIn.price, OrderType.Buy, lotSizeChooser(tickIn.price, TradeSignal.OpenBuy)))
    case Some(TradeSignal.OpenSell) =>
      List(Order(orderId, tickIn.price, OrderType.Sell, lotSizeChooser(tickIn.price, TradeSignal.OpenSell)))
  }

  def moneyFromClosedOrders(closedOrders: List[Order], currentPrice: Price) = closedOrders.map { order =>
    val diff = (BigDecimal(currentPrice.close.cents) - BigDecimal(order.openPrice)) * BigDecimal(order.size)
    order.orderType match {
      case OrderType.Buy => diff
      case OrderType.Sell => - diff
    }
  }.sum

  def moneyForOpeningOrders(orders: List[Order]) = orders
    .map(order => BigDecimal(order.openPrice) * BigDecimal(order.size))
    .sum

  def potentialProfit(openOrders: List[Order], currentPrice: Price) = openOrders.map { order =>
    val diff = (BigDecimal(currentPrice.close.cents) - BigDecimal(order.openPrice)) * BigDecimal(order.size)
    order.orderType match {
      case OrderType.Buy => order.openPrice + diff
      case OrderType.Sell => order.openPrice - diff
    }
  }.sum

  override def receive: Receive = withState(Account(initialBalance, equity = Money.zero), List.empty[Order])

  def withState(previousAccState: Account, previousOpenOrders: List[Order]): Receive = {
    case tickIn @ TickIn(currentPrice, signalOption) =>
      val justClosedOrders = List.empty[Order] // todo implement closing orders
      val justOpenedOrders = openNewOrders(tickIn)

      val newOpenOrders = previousOpenOrders ++ justOpenedOrders
      val newBalance = previousAccState.balance.cents + moneyFromClosedOrders(justClosedOrders, currentPrice) - moneyForOpeningOrders(justOpenedOrders)
      val newEquity = newBalance + potentialProfit(newOpenOrders, currentPrice)
      val newAccState = Account(Money(newBalance.toLong), Money(newEquity.toLong))

      context.become(withState(newAccState, newOpenOrders))

      sender() ! TickOut(newAccState,
        events = justClosedOrders.map(TradeEvent.OrderClosed) ++ justOpenedOrders.map(TradeEvent.OrderOpened))
  }

}
