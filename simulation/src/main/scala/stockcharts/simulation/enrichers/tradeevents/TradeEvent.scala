package stockcharts.simulation.enrichers.tradeevents

import stockcharts.models.{Money, Price}
import stockcharts.simulation.enrichers.tradesignals.TradeSignal

case class Account(balance: Money, equity: Money)

case class TickIn(price: Price, tradeSignal: Option[TradeSignal])
case class TickOut(price: Price, account: Account, events: List[TradeEvent])

sealed trait TradeEvent
object TradeEvent {
  case class OrderOpened(order: Order) extends TradeEvent
  case class OrderClosed(order: Order, closePrice: Price, balanceChange: Money) extends TradeEvent
}

case class Order(id: Long, price: Price, orderType: OrderType, size: Int) {
  val openPrice = price.close.cents
}

sealed trait OrderType
object OrderType {
  object Sell extends OrderType
  object Buy extends OrderType
}
