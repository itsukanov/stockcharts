package stockcharts.enrichers.indicators

import akka.actor.Props
import eu.verdelhan.ta4j.{Decimal, TimeSeries}
import eu.verdelhan.ta4j.indicators.helpers.ClosePriceIndicator

class Indicator[Pure, Out](maximumTickCount: Int,
                           indicatorMaker: TimeSeries => eu.verdelhan.ta4j.Indicator[Pure],
                           converter: Pure => Out) {

  def props() = Props(new IndicatorCalculations(
    maximumTickCount,
    indicatorMaker,
    converter
  ))
}

case class RSIValue(v: Double) extends AnyVal

object RSIIndicator {

  def apply(period: Int) = new Indicator(
    maximumTickCount = period,
    indicatorMaker = series => new eu.verdelhan.ta4j.indicators.RSIIndicator(new ClosePriceIndicator(series), period),
    converter = (decimal: Decimal) => RSIValue(decimal.toDouble)
  )

}

object SMAIndicator {

  def apply(period: Int) = new Indicator(
    maximumTickCount = period,
    indicatorMaker = series => new eu.verdelhan.ta4j.indicators.SMAIndicator(new ClosePriceIndicator(series), period),
    converter = (decimal: Decimal) => decimal.toDouble
  )

}

