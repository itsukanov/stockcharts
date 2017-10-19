package stockcharts.enrichers.tradesignals

import akka.actor.{Actor, Props}

class OverBoughtSoldStrategy[T: Numeric](overBoughtLevel: T, overSoldLevel: T) extends Actor {
  val num = implicitly[Numeric[T]]
  require(num.gt(overBoughtLevel, overSoldLevel))

  trait Level
  case object Top extends Level
  case object Middle extends Level
  case object Bottom extends Level

  def levelOf(value: T) =
    if (num.gt(value, overBoughtLevel)) Top
    else if (num.lt(value, overSoldLevel)) Bottom
    else Middle

  def strategy(previous: T): Receive = {
    case current: T =>
      context.become(strategy(current))

      val tradeSignal = (levelOf(previous), levelOf(current)) match {
        case (Top, Middle) => Some(TradeSignal.OpenSell)
        case (Bottom, Middle) => Some(TradeSignal.OpenBuy)
        case _ => None
      }

      sender() ! tradeSignal
  }

  override def receive: Receive = {
    case value: T =>
      context.become(strategy(value))
      sender() ! Option.empty[TradeSignal]
  }

}

object OverBoughtSoldStrategy {

  def apply[T: Numeric](overBoughtLevel: T, overSoldLevel: T) = new TradeInSignalsStrategy {
    override def props: Props = Props(new OverBoughtSoldStrategy(overBoughtLevel, overSoldLevel))
  }

}

