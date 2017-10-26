package stockcharts.simulation.enrichers.tradesignals

import akka.actor.Props

sealed trait TradeSignal
object TradeSignal {
  case object OpenBuy extends TradeSignal
  case object OpenSell extends TradeSignal
}

trait TradeInSignalsStrategy {
  def props: Props
}
