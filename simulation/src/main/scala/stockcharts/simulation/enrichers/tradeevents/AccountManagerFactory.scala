package stockcharts.simulation.enrichers.tradeevents

import akka.actor.{Actor, Props}
import stockcharts.simulation.enrichers.tradesignals.TradeSignal
import stockcharts.models.{Money, Price}

trait AccountManagerFactory {
  def props: Props
}

object AccountManager {
  def apply(initialBalance: Money,
            lotSizeChooser: (Price, TradeSignal) => Int,
            isOrderReadyToClose: (Order, Price) => Boolean) = new AccountManagerFactory {

    override def props: Props = Props(new AccountManager(initialBalance, lotSizeChooser, isOrderReadyToClose))
  }
}

class AccountManager(initialBalance: Money,
                     lotSizeChooser: (Price, TradeSignal) => Int,
                     isOrderReadyToClose: (Order, Price) => Boolean
                    ) extends Actor {

  var lastId = 0L
  def orderId = {
    lastId += 1
    lastId
  }

  def openNewOrders(tickIn: TickIn, balance: Money) = {
    val lotSize = lotSizeChooser(tickIn.price, TradeSignal.OpenBuy)
    val orderPrice = lotSize * tickIn.price.close.cents
    val canAfford = orderPrice <= balance.cents

    tickIn.tradeSignal match {
      case Some(TradeSignal.OpenBuy) if canAfford =>
        List(Order(orderId, tickIn.price, OrderType.Buy, lotSize))
      case Some(TradeSignal.OpenSell) if canAfford =>
        List(Order(orderId, tickIn.price, OrderType.Sell, lotSize))
      case _ => List.empty[Order]
    }
  }

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
      val (justClosedOrders, stillOpen) = previousOpenOrders.partition(order => isOrderReadyToClose(order, currentPrice))
      val moneyFromClosedOrders = potentialProfit(justClosedOrders, currentPrice)
      val justOpenedOrders = openNewOrders(tickIn, balance =
        previousAccState.balance + Money(moneyFromClosedOrders.toLong))

      val newOpenOrders = stillOpen ++ justOpenedOrders
      val newBalance = previousAccState.balance.cents + moneyFromClosedOrders - moneyForOpeningOrders(justOpenedOrders)
      val newEquity = newBalance + potentialProfit(newOpenOrders, currentPrice)
      val newAccState = Account(Money(newBalance.toLong), Money(newEquity.toLong))

      context.become(withState(newAccState, newOpenOrders))

      sender() ! TickOut(currentPrice, newAccState,
        events = justClosedOrders.map(order => TradeEvent.OrderClosed(order, currentPrice))
          ++ justOpenedOrders.map(TradeEvent.OrderOpened))
  }

}
