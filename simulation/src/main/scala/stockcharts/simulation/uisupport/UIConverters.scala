package stockcharts.simulation.uisupport

import akka.stream.scaladsl.Flow
import stockcharts.models.{Money, Price}
import stockcharts.simulation.enrichers.indicators.RSIValue

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

  implicit val priceConverter = new UIConverter[Price, UIPrice] {
    override def convert(from: Price): UIPrice = UIPrice(from.date, from.open, from.high, from.low, from.close)
  }

  implicit val rsiConverter = new UIConverter[RSIValue, UIIndicatorValue] {
    override def convert(from: RSIValue): UIIndicatorValue = UIIndicatorValue(from.date, from.value)
  }

}
