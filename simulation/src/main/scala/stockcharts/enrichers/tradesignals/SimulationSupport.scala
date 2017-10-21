package stockcharts.enrichers.tradesignals

import akka.actor.{ActorRefFactory, PoisonPill}
import akka.pattern.ask
import akka.stream.scaladsl.Source
import akka.util.Timeout
import stockcharts.enrichers.tradeevents.{AccountManagerFactory, Order, TickIn, TickOut}
import stockcharts.models.Price

object SimulationSupport {

  def constantSizeLotChooser(lotSize: Int = 1) = (price: Price, signal: TradeSignal, openOrders: List[Order]) => lotSize

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
