package stockcharts.simulation

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import org.slf4j.LoggerFactory
import stockcharts.Config.Simulation

import scala.util.{Failure, Success}

object SimulationApp extends App {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val system = ActorSystem("simulation-app")
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  val binding = Http().bindAndHandle(Routing(SimulationFactory.apply), Simulation.host, Simulation.port)
  binding.onComplete {
    case Success(_) => log.info("Rest api binding has been successfully done")
    case Failure(thr) =>
      log.error("Rest api binding has been failed", thr)
      sys.exit(1)
  }

}
