package stockcharts.simulation.enrichers.indicators

import java.util

import akka.actor.Actor
import eu.verdelhan.ta4j.{BaseTick, BaseTimeSeries, Tick, TimeSeries}

class IndicatorCalculations[Pure, Out](maximumTickCount: Int,
                                       indicatorMaker: TimeSeries => eu.verdelhan.ta4j.Indicator[Pure],
                                       outputConverter: Pure => Out) extends Actor {

  val series = new BaseTimeSeries(new util.ArrayList[Tick]())
  series.setMaximumTickCount(maximumTickCount)

  val indicator = indicatorMaker(series)

  override def receive: Receive = {
    case tick: BaseTick =>
      series.addTick(tick)
      sender() ! outputConverter(indicator.getValue(series.getEndIndex))
  }
}
