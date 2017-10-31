package stockcharts.simulation

import java.util.UUID

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Source}
import org.json4s.native.JsonMethods._
import org.slf4j.LoggerFactory
import stockcharts.Config.Stocks
import stockcharts.models.{Money, Price, SimulationConf, Stock}
import stockcharts.simulation.uisupport.{InvalidConfig, SimulationDone, UIModel}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

trait PriceSourceFactory {
  def prices(stock: Stock, groupId: String): Source[Price, _]
}

case class SimulationRequest(stock: String,
                             overbought: String,
                             oversold: String,
                             takeProfit: String,
                             stopLoss: String)

class Routing(simulationFactory: SimulationConf => Flow[Price, UIModel, _],
              priceSourceFactory: PriceSourceFactory,
              uiSerializer: Flow[UIModel, String, _]) {

  private val log = LoggerFactory.getLogger(this.getClass)
  private implicit val formats = org.json4s.DefaultFormats

  private def tryParseConf(confAsStr: String) =
    Try {
      val request = parse(confAsStr).extract[SimulationRequest]
      val stock = Stocks.getById(request.stock).getOrElse(throw new IllegalArgumentException("invalid stockId"))

      SimulationConf(
        simulationId = UUID.randomUUID().toString, stock, request.overbought.toDouble, request.oversold.toDouble,
        takeProfit = Money(request.takeProfit.toDouble * 100 toLong), stopLoss = Money(request.stopLoss.toDouble * 100 toLong))
    }

  val wsCommandHandler =
    Flow[Message].flatMapConcat {
      case TextMessage.Strict(confAsStr) =>
        tryParseConf(confAsStr) match {
          case Success(conf) =>
            val simulation = priceSourceFactory.prices(conf.stock, conf.simulationId)
              .via(simulationFactory(conf))
              .idleTimeout(5 seconds)
              .recover {
                case ex: scala.concurrent.TimeoutException => SimulationDone()
                case thr =>
                  log.error("Error during simulation", thr)
                  throw thr
              }.via(uiSerializer)
              .map(TextMessage.apply)

            log.debug(s"Trade simulation for $conf has been created")
            simulation

          case Failure(thr) =>
            log.warn(s"Can't extract simulation conf from '$confAsStr'", thr)
            Source
              .single(InvalidConfig(s"Can't extract simulation config from '$confAsStr' because of:\n${thr.toString}"))
              .via(uiSerializer)
              .map(TextMessage.apply)
        }

      case _ => Source.single(TextMessage("Message unsupported"))
    }

  val route =
    path("simulate") {
      handleWebSocketMessages(wsCommandHandler)
    }

}
