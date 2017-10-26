package stockcharts.simulation.uisupport

import stockcharts.models.{Money, Price}
import stockcharts.simulation.enrichers.indicators.RSIValue

import scala.language.implicitConversions

trait UIConverter[From, To <: UIModel] {
  def convert(from: From): To
}

object UIConverters {

  def toUIModel[From, To <: UIModel](from: From)(implicit converter: UIConverter[From, To]) =
    converter.convert(from)

  implicit def money2Double(money: Money): Double = money.cents.toDouble / 100

  implicit val priceConverter = new UIConverter[Price, UIPrice] {
    override def convert(from: Price): UIPrice = UIPrice(from.date, from.open, from.high, from.low, from.close)
  }

  implicit val rsiConverter = new UIConverter[RSIValue, UIIndicatorValue] {
    override def convert(from: RSIValue): UIIndicatorValue = UIIndicatorValue(from.value)
  }

}
