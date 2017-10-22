package stockcharts.enrichers.tradeevents

import stockcharts.enrichers.tradesignals.TradeSignal
import stockcharts.models.{Money, Price}

case class Account(balance: Money, equity: Money)

case class TickIn(price: Price, tradeSignal: Option[TradeSignal])
case class TickOut(account: Account, events: List[TradeEvent])

trait TradeEvent
object TradeEvent {
  case class OrderOpened(order: Order) extends TradeEvent
  case class OrderClosed(order: Order) extends TradeEvent
}

case class Order(id: Long, price: Price, orderType: OrderType, size: Int) {
  val openPrice = price.close.cents
}

trait OrderType
object OrderType {
  object Sell extends OrderType
  object Buy extends OrderType
}
