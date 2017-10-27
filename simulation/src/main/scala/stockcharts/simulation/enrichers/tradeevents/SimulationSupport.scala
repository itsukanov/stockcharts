package stockcharts.simulation.enrichers.tradeevents

import akka.NotUsed
import akka.actor.{ActorRefFactory, PoisonPill}
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import stockcharts.models.{Money, Price}
import stockcharts.simulation.enrichers.tradesignals.TradeSignal

object SimulationSupport {

  def constantSizeLotChooser(lotSize: Int = 1) = (price: Price, signal: TradeSignal) => lotSize

  def takeProfitStopLossChecker(takeProfit: Money, stopLoss: Money) = (order: Order, currentPrice: Price) => {
    val diff = currentPrice.close.cents - order.openPrice
    order.orderType match {
      case OrderType.Buy => diff >= takeProfit.cents || - diff >= stopLoss.cents
      case OrderType.Sell => - diff >= takeProfit.cents || diff >= stopLoss.cents
    }
  }

  def calculateAccountChanges(accountManagerFactory: AccountManagerFactory)
          (implicit arf: ActorRefFactory, to: Timeout): Flow[TickIn, TickOut, NotUsed] = {
    import arf.dispatcher
    val accManager = arf.actorOf(accountManagerFactory.props)

    Flow[TickIn]
      .mapAsync(1)(tick => accManager ? tick)
      .map(_.asInstanceOf[TickOut])
      .watchTermination() { case (mat, futureDone) =>
        futureDone.onComplete(_ => accManager ! PoisonPill)
        mat
      }
  }

}
