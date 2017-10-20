package stockcharts.enrichers.tradeevents

import stockcharts.enrichers.tradesignals.TradeSignal
import stockcharts.models.Price

case class Account(balance: Double, equity: Double)

case class TickIn(price: Price, tradeSignal: Option[TradeSignal])
case class TickOut(account: Account, events: Set[TradeEvent])

trait TradeEvent
object TradeEvent {
  case class OrderOpened(order: Order) extends TradeEvent
  case class OrderClosed(order: Order) extends TradeEvent
}

case class Order(id: Long, openPrice: Price, orderType: OrderType, size: Int)

trait OrderType
object OrderType {
  object Sell extends OrderType
  object Buy extends OrderType
}
