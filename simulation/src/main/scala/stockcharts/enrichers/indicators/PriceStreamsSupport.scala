package stockcharts.enrichers.indicators

import java.time.ZoneOffset

import akka.actor.{ActorRefFactory, PoisonPill}
import akka.pattern._
import akka.stream.scaladsl.Source
import akka.util.Timeout
import eu.verdelhan.ta4j.BaseTick
import stockcharts.models.Price

import scala.concurrent.duration._
import scala.language.postfixOps


object PriceStreamsSupport {

  implicit class PriceStream[Mat](prices: Source[Price, Mat]) {

    implicit private val to = Timeout(3 seconds)

    def calculate[Out](indicator: Indicator[_, Out])(implicit arf: ActorRefFactory): Source[Out, Mat] = {
      val tick2IndicatorValue = arf.actorOf(indicator.props())
      import arf.dispatcher

      prices.map(price => new BaseTick(price.date.atStartOfDay(ZoneOffset.UTC), price.open, price.high, price.low, price.close, 0))
        .mapAsync(1)(tick => tick2IndicatorValue ? tick)
        .map(_.asInstanceOf[Out])
        .watchTermination() { case (mat, futureDone) =>
          futureDone.onComplete(_ => tick2IndicatorValue ! PoisonPill)
          mat
        }
    }
  }

}
