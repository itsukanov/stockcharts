package stockcharts.enrichers.tradeevents

import akka.actor.{Actor, Props}
import stockcharts.enrichers.tradesignals.TradeSignal
import stockcharts.models.Price

trait AccountManagerFactory {
  def props: Props
}

object AccountManager {
  def apply(initialBalance: Double,
            lotSizeChooser: (Price, TradeSignal, List[Order]) => Int) = new AccountManagerFactory {

    override def props: Props = Props(new AccountManager(initialBalance, lotSizeChooser))
  }
}

class AccountManager(initialBalance: Double,
                     lotSizeChooser: (Price, TradeSignal, List[Order]) => Int) extends Actor {

  var lastAcc = Account(initialBalance, equity = 0)
  var openOrders = List.empty[Order]

  var lastId = 0L
  def orderId = {
    lastId += 1
    lastId
  }

  override def receive: Receive = {
    case TickIn(currentPrice, signalOption) =>
      val newClosed = List.empty[Order]
      val newOpened = signalOption match {
        case None => Nil
        case Some(TradeSignal.OpenBuy) =>
          List(Order(orderId, currentPrice, OrderType.Buy, lotSizeChooser(currentPrice, TradeSignal.OpenBuy, openOrders)))
        case Some(TradeSignal.OpenSell) =>
          List(Order(orderId, currentPrice, OrderType.Sell, lotSizeChooser(currentPrice, TradeSignal.OpenSell, openOrders)))
      }

      val changeFromClosed = newClosed.map { order =>
        val diff = (BigDecimal(currentPrice.close.cents) - BigDecimal(order.openPrice)) * BigDecimal(order.size)
        order.orderType match {
          case OrderType.Buy => diff
          case OrderType.Sell => - diff
        }
      }.sum.toDouble

      val changeFromOpened = newOpened
        .map(order => BigDecimal(order.openPrice) * BigDecimal(order.size))
        .sum.toDouble

      val newBalance = lastAcc.balance + changeFromClosed - changeFromOpened

      openOrders = openOrders ++ newOpened

      val newEquity = openOrders.map { order =>
        val diff = (BigDecimal(currentPrice.close.cents) - BigDecimal(order.openPrice)) * BigDecimal(order.size)
        order.orderType match {
          case OrderType.Buy => order.openPrice + diff
          case OrderType.Sell => order.openPrice - diff
        }
      }.sum.toDouble + newBalance

      lastAcc = lastAcc.copy(balance = newBalance, equity = newEquity)
      val tradeEvents = newClosed.map(TradeEvent.OrderClosed) ++ newOpened.map(TradeEvent.OrderOpened)

      sender() ! TickOut(lastAcc, tradeEvents)
  }

}