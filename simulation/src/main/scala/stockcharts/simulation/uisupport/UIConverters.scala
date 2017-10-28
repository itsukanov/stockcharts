package stockcharts.simulation.uisupport

import akka.stream.scaladsl.Flow
import stockcharts.models.{Money, Price}
import stockcharts.simulation.enrichers.indicators.RSIValue
import stockcharts.simulation.enrichers.tradeevents.{OrderType, TickOut, TradeEvent}

import scala.language.implicitConversions

trait UIConverter[From, To <: UIModel] {
  def convert(from: From): To
}

object UIConverters {

  class TypeHolder[T]
  def toUI[To <: UIModel] = new TypeHolder[To]

  implicit def holder2Flow[From, To <: UIModel](th: TypeHolder[To])
                                               (implicit converter: UIConverter[From, To]): Flow[From, To, _] =
    Flow[From].map(converter.convert)


  implicit def money2Double(money: Money): Double = money.cents.toDouble / 100
  implicit def orderType2String(orderType: OrderType): String = orderType match {
    case OrderType.Buy => "buy"
    case OrderType.Sell => "sell"
  }

  implicit val priceConverter = new UIConverter[Price, UIPrice] {
    override def convert(from: Price): UIPrice = UIPrice(from.date, from.open, from.high, from.low, from.close)
  }

  implicit val rsiConverter = new UIConverter[RSIValue, UIIndicatorValue] {
    override def convert(from: RSIValue): UIIndicatorValue = UIIndicatorValue(from.date, from.value)
  }

  implicit val accountConverter = new UIConverter[TickOut, UIAccount] {
    override def convert(from: TickOut): UIAccount = UIAccount(from.price.date, from.account.balance, from.account.equity)
  }

  implicit val tradeEventsConverter = new UIConverter[TradeEvent, UITradeEvent] {
    override def convert(from: TradeEvent): UITradeEvent = from match {
      case TradeEvent.OrderOpened(order) => UITradeEvent.UIOrderOpened(order.id, openDate = order.price.date, order.orderType)
      case TradeEvent.OrderClosed(order, closePrice) => UITradeEvent.UIOrderClosed(
        id = order.id,
        openDate = order.price.date,
        closeDate = closePrice.date,
        openPrice = order.price.close,
        closePrice = closePrice.close,
        balanceChange = 42,
        orderType = order.orderType)
    }
  }

}
