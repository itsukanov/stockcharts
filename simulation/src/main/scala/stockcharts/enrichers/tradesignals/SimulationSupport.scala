package stockcharts.enrichers.tradesignals

import akka.actor.{ActorRefFactory, PoisonPill}
import akka.pattern.ask
import akka.stream.scaladsl.Source
import akka.util.Timeout
import stockcharts.enrichers.tradeevents.{AccountManagerFactory, Order, OrderType, TickIn, TickOut}
import stockcharts.models.{Money, Price}

object SimulationSupport {

  def constantSizeLotChooser(lotSize: Int = 1) = (price: Price, signal: TradeSignal) => lotSize

  def takeProfitStopLossChecker(takeProfit: Money, stopLoss: Money) = (order: Order, currentPrice: Price) => {
    val diff = currentPrice.close.cents - order.openPrice
    order.orderType match {
      case OrderType.Buy => diff >= takeProfit.cents || - diff >= stopLoss.cents
      case OrderType.Sell => - diff >= takeProfit.cents || diff >= stopLoss.cents
    }
  }

  def calculateAccountChanges[Mat](ticks: Source[TickIn, Mat],
                                   accountManagerFactory: AccountManagerFactory)
                                  (implicit arf: ActorRefFactory, to: Timeout): Source[TickOut, Mat] = {
    import arf.dispatcher
    val accManager = arf.actorOf(accountManagerFactory.props)

    ticks
      .mapAsync(1)(tick => accManager ? tick)
      .map(_.asInstanceOf[TickOut])
      .watchTermination() { case (mat, futureDone) =>
        futureDone.onComplete(_ => accManager ! PoisonPill)
        mat
      }
  }

}
