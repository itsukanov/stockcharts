package stockcharts.simulation.enrichers.indicators

import java.time.LocalDate

import akka.actor.Props
import eu.verdelhan.ta4j.{Decimal, TimeSeries}
import eu.verdelhan.ta4j.indicators.helpers.ClosePriceIndicator
import stockcharts.models.Price

class Indicator[Pure, Out](maximumTickCount: Int,
                           indicatorMaker: TimeSeries => eu.verdelhan.ta4j.Indicator[Pure],
                           converter: (Price, Pure) => Out) {

  def props() = Props(new IndicatorCalculations(
    maximumTickCount,
    indicatorMaker,
    converter
  ))
}

case class RSIValue(date: LocalDate, value: Double)

object RSIIndicator {

  def apply(period: Int) = new Indicator(
    maximumTickCount = period,
    indicatorMaker = series => new eu.verdelhan.ta4j.indicators.RSIIndicator(new ClosePriceIndicator(series), period),
    converter = (price: Price, decimal: Decimal) => RSIValue(price.date, decimal.toDouble)
  )

}

object SMAIndicator {

  def apply(period: Int) = new Indicator(
    maximumTickCount = period,
    indicatorMaker = series => new eu.verdelhan.ta4j.indicators.SMAIndicator(new ClosePriceIndicator(series), period),
    converter = (price: Price, decimal: Decimal) => decimal.toDouble
  )

}

