package stockcharts.simulation

import java.util.UUID

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Source}
import org.json4s.native.JsonMethods._
import org.slf4j.LoggerFactory
import stockcharts.Config.Stocks
import stockcharts.json.JsonConverting
import stockcharts.models.SimulationConf

import scala.util.{Failure, Success, Try}

object Routing {

  private val log = LoggerFactory.getLogger(this.getClass)
  private implicit val formats = org.json4s.DefaultFormats

  private def tryParseConf(confAsStr: String) = {
    case class SimulationRequest(stock: String,
                                 rsiBuy: Double,
                                 rsiSell: Double,
                                 takeProfit: Double,
                                 stopLoss: Double)

    Try {
      val request = parse(confAsStr).extract[SimulationRequest]
      val stock = Stocks.getById(request.stock).getOrElse(throw new IllegalArgumentException("invalid stockId"))

      SimulationConf(
        simulationId = UUID.randomUUID().toString, stock, request.rsiBuy, request.rsiSell, request.takeProfit, request.stopLoss)
    }
  }

  private case class SystemMessage(`type`: String)
  private val done = Source.single(SystemMessage("Simulation done")).via(JsonConverting.toJsonFlow)

  def wsCommandHandler(simulationFactory: SimulationConf => Source[String, _]) =
    Flow[Message].flatMapConcat {
      case TextMessage.Strict(confAsStr) =>
        tryParseConf(confAsStr) match {
          case Success(conf) =>
            val simulation = simulationFactory(conf).take(100) // todo delete 'take'
              .concat(done)
              .map(TextMessage.apply)
            log.debug(s"Trade simulation for $conf has been created")
            simulation

          case Failure(thr) =>
            log.warn(s"Can't extract simulation conf from '$confAsStr'", thr)
            Source.single(TextMessage(s"Can't extract simulation conf from '$confAsStr' because:\n${thr.getMessage}"))
        }

      case _ => Source.single(TextMessage("Message unsupported"))
    }

  def apply(simulationFactory: SimulationConf => Source[String, _]) =
    path("simulate") {
      handleWebSocketMessages(wsCommandHandler(simulationFactory))
    }

}
