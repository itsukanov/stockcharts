package stockcharts.simulation

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.slf4j.LoggerFactory
import stockcharts.Config.RSIStrategyConf._
import stockcharts.Config.SimulationAppConf
import stockcharts.json.JsonConverting

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object SimulationApp extends App {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val system = ActorSystem("simulation-app")
  import system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val to = Timeout(3 seconds)

  val binding = Http().bindAndHandle(Routing(
    simulationFactory = SimulationFactory.simulateTrade(rsiPeriod, initialBalance, lotSize),
    uiSerializer = JsonConverting.toJsonFlow), SimulationAppConf.host, SimulationAppConf.port)
  binding.onComplete {
    case Success(_) => log.info("Rest api binding has been successfully done")
    case Failure(thr) =>
      log.error("Rest api binding has been failed", thr)
      sys.exit(1)
  }

}
