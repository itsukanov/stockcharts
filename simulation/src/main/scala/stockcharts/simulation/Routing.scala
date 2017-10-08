package stockcharts.simulation

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Flow
import org.slf4j.LoggerFactory
import stockcharts.Config.Stocks
import stockcharts.models.SimulationConf

object Routing {

  val log = LoggerFactory.getLogger(this.getClass)

  def apply(simulationFactory: SimulationConf => Flow[Message, Message, NotUsed]) =
    path("simulate") {
      parameters('stock,
        'rsiBuy.as[Double],
        'rsiSell.as[Double],
        'takeProfit.as[Double],
        'stopLoss.as[Double]) {
        (stockId, rsiBuy, rsiSell, takeProfit, stopLoss) =>
          val stock = Stocks.getById(stockId).getOrElse(throw new RuntimeException("invalid stockId")) // todo change to custom extractor
          val simulationConf = SimulationConf(stock, rsiBuy, rsiSell, takeProfit, stopLoss)
          val tradeSimulation = simulationFactory(simulationConf)

          log.debug(s"Trade simulation for $simulationConf has been created")
          handleWebSocketMessages(tradeSimulation)
      }
    }

}
