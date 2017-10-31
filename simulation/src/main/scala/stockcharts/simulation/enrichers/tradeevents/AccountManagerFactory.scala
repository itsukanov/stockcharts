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

  def potentialBalanceChange(orders: List[Order], currentPrice: Price) = {
    val openingCost = orders.map(_.openPrice).sum
    val changesFromOrders = potentialOrderProfits(orders, currentPrice).sum
    openingCost + changesFromOrders
  }

  def potentialOrderProfits(orders: List[Order], currentPrice: Price) = orders.map { order =>
    val diff = (BigDecimal(currentPrice.close.cents) - BigDecimal(order.openPrice)) * BigDecimal(order.size)
    order.orderType match {
      case OrderType.Buy => diff
      case OrderType.Sell => - diff
    }
  }

  def isMarginCallCase(balance: Money, currentPrice: Price, openOrders: List[Order]) = {
    val equity = balance.cents + potentialBalanceChange(openOrders, currentPrice)
    equity <= 0
  }

  override def receive: Receive = withState(Account(initialBalance, equity = Money.zero), List.empty[Order])

  def withState(accState: Account, openOrders: List[Order]): Receive = {
    case TickIn(currentPrice, _) if isMarginCallCase(accState.balance, currentPrice, openOrders) =>
      val changesFromClosedOrders = potentialBalanceChange(openOrders, currentPrice)

      val newBalance = accState.balance.cents + changesFromClosedOrders
      val newEquity = newBalance // all orders were closed
      val newAccState = Account(Money(newBalance.toLong), Money(newEquity.toLong))

      val tradeEvents = openOrders zip potentialOrderProfits(openOrders, currentPrice) map {
        case (order, balanceChange) =>
          TradeEvent.OrderClosed(order, currentPrice, Money(balanceChange.toLong))
      }
      context.become(withState(newAccState, Nil))
      sender() ! TickOut(currentPrice, newAccState, tradeEvents)

    case tickIn @ TickIn(currentPrice, signalOption) => // todo simplify calculations
      val (justClosedOrders, stillOpen) = openOrders.partition(order => isOrderReadyToClose(order, currentPrice))
      val changesFromClosedOrders = potentialBalanceChange(justClosedOrders, currentPrice)
      val justOpenedOrders = openNewOrders(tickIn, accState.balance + Money(changesFromClosedOrders.toLong))

      val newOpenOrders = stillOpen ++ justOpenedOrders
      val newBalance = accState.balance.cents + changesFromClosedOrders - moneyForOpeningOrders(justOpenedOrders)
      val newEquity = newBalance + potentialBalanceChange(newOpenOrders, currentPrice)
      val newAccState = Account(Money(newBalance.toLong), Money(newEquity.toLong))

      val tradeEvents = {
        val orderClosedEvents = justClosedOrders zip potentialOrderProfits(justClosedOrders, currentPrice) map {
          case (order, balanceChange) =>
            TradeEvent.OrderClosed(order, currentPrice, Money(balanceChange.toLong))
        }
        justOpenedOrders.map(TradeEvent.OrderOpened) ++ orderClosedEvents
      }

      context.become(withState(newAccState, newOpenOrders))
      sender() ! TickOut(currentPrice, newAccState, tradeEvents)
  }

}
