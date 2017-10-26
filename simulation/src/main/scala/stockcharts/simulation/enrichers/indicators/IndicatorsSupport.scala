package stockcharts.simulation.enrichers.indicators

import java.time.ZoneOffset

import akka.NotUsed
import akka.actor.{ActorRefFactory, PoisonPill}
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import eu.verdelhan.ta4j.BaseTick
import stockcharts.models.Price
import akka.pattern.ask

object IndicatorsSupport {

  def calculating[Out](indicator: Indicator[_, Out])(implicit arf: ActorRefFactory, to: Timeout): Flow[Price, Out, NotUsed] = {
    val tick2IndicatorValue = arf.actorOf(indicator.props())
    import arf.dispatcher

    Flow[Price].map(price => new BaseTick(price.date.atStartOfDay(ZoneOffset.UTC), price.open.cents, price.high.cents, price.low.cents, price.close.cents, 0))
      .mapAsync(1)(tick => tick2IndicatorValue ? tick)
      .map(_.asInstanceOf[Out])
      .watchTermination() { case (mat, futureDone) =>
        futureDone.onComplete(_ => tick2IndicatorValue ! PoisonPill)
        mat
      }
  }

}
