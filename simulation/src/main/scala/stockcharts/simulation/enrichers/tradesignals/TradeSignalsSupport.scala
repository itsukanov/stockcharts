package stockcharts.simulation.enrichers.tradesignals

import akka.NotUsed
import akka.actor.{ActorRefFactory, PoisonPill}
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout

object TradeSignalsSupport {

  def calculateTradeSignals[T: Numeric](strategy: TradeInSignalsStrategy)
                           (implicit arf: ActorRefFactory, to: Timeout): Flow[T, Option[TradeSignal], NotUsed] = {
    val value2TradeSignal = arf.actorOf(strategy.props)
    import arf.dispatcher

    Flow[T]
      .mapAsync(1)(value => value2TradeSignal ? value)
      .map(_.asInstanceOf[Option[TradeSignal]])
      .watchTermination() { case (mat, futureDone) =>
        futureDone.onComplete(_ => value2TradeSignal ! PoisonPill)
        mat
      }
  }
}
