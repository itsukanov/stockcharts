package stockcharts.simulation

import java.util.UUID

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Source}
import org.slf4j.LoggerFactory
import stockcharts.Config.Stocks
import stockcharts.models.SimulationConf

object Routing {

  val log = LoggerFactory.getLogger(this.getClass)

  def wsCommandHandler(simulation: Source[String, _]) =
    Flow[Message].flatMapConcat {
      case TextMessage.Strict("simulate") => // todo stop stream after simulation will be finished
        simulation.map(TextMessage.apply)

      case _ => Source.single(TextMessage("Message unsupported"))
    }

  def apply(simulationFactory: SimulationConf => Source[String, _]) =
    path("simulate") {
      parameters('stock,
        'rsiBuy.as[Double],
        'rsiSell.as[Double],
        'takeProfit.as[Double],
        'stopLoss.as[Double]) {
        (stockId, rsiBuy, rsiSell, takeProfit, stopLoss) =>
          val stock = Stocks.getById(stockId).getOrElse(throw new RuntimeException("invalid stockId")) // todo change to custom extractor
          val simulationConf = SimulationConf(
            simulationId = UUID.randomUUID().toString, stock, rsiBuy, rsiSell, takeProfit, stopLoss)
          val tradeSimulation = simulationFactory(simulationConf)

          log.debug(s"Trade simulation for $simulationConf has been created")
          handleWebSocketMessages(wsCommandHandler(tradeSimulation))
      }
    }

}
