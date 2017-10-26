package stockcharts.simulation.enrichers.indicators

import java.time.ZoneOffset
import java.util

import akka.actor.Actor
import eu.verdelhan.ta4j.{BaseTick, BaseTimeSeries, Tick, TimeSeries}
import stockcharts.models.Price

class IndicatorCalculations[Pure, Out](maximumTickCount: Int,
                                       indicatorMaker: TimeSeries => eu.verdelhan.ta4j.Indicator[Pure],
                                       outputConverter: (Price, Pure) => Out) extends Actor {

  val series = new BaseTimeSeries(new util.ArrayList[Tick]())
  series.setMaximumTickCount(maximumTickCount)

  val indicator = indicatorMaker(series)

  override def receive: Receive = {
    case price: Price =>
      val tick = new BaseTick(price.date.atStartOfDay(ZoneOffset.UTC), price.open.cents, price.high.cents, price.low.cents, price.close.cents, 0)
      series.addTick(tick)
      sender() ! outputConverter(price, indicator.getValue(series.getEndIndex))
  }
}
