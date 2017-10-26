package stockcharts.simulation.enrichers

import akka.actor.{ActorRefFactory, PoisonPill}
import akka.pattern.ask
import akka.stream.scaladsl.Source
import akka.util.Timeout
import stockcharts.simulation.enrichers.tradesignals.{TradeInSignalsStrategy, TradeSignal}

object NumericStreamsSupport {

  implicit class NumericStream[Mat, T: Numeric](values: Source[T, Mat]) {

    def calculateTradeSignals(strategy: TradeInSignalsStrategy)
                             (implicit arf: ActorRefFactory, to: Timeout): Source[Option[TradeSignal], Mat] = {
      val value2TradeSignal = arf.actorOf(strategy.props)
      import arf.dispatcher

      values
        .mapAsync(1)(value => value2TradeSignal ? value)
        .map(_.asInstanceOf[Option[TradeSignal]])
        .watchTermination() { case (mat, futureDone) =>
          futureDone.onComplete(_ => value2TradeSignal ! PoisonPill)
          mat
        }
    }
  }

}
