package stockcharts.simulation.enrichers.indicators

import akka.NotUsed
import akka.actor.{ActorRefFactory, PoisonPill}
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import stockcharts.models.Price

object IndicatorsSupport {

  def calculating[Out](indicator: Indicator[_, Out])(implicit arf: ActorRefFactory, to: Timeout): Flow[Price, Out, NotUsed] = {
    val price2IndicatorValue = arf.actorOf(indicator.props())
    import arf.dispatcher

    Flow[Price]
      .mapAsync(1)(price => price2IndicatorValue ? price)
      .map(_.asInstanceOf[Out])
      .watchTermination() { case (mat, futureDone) =>
        futureDone.onComplete(_ => price2IndicatorValue ! PoisonPill)
        mat
      }
  }

}
