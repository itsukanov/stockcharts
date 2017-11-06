package stockcharts.ui

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import org.slf4j.LoggerFactory
import stockcharts.Config.{SimulationAppConf, UI}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object UIApp extends App {

  val log = LoggerFactory.getLogger(this.getClass)
  val simulationUrl = s"${SimulationAppConf.serverUrl}/simulate"

  implicit val system = ActorSystem("ui-app")
  implicit val materializer = ActorMaterializer()

  val binding = Http().bindAndHandle(new Routing(simulationUrl).route, UI.host, UI.port)
  binding.onComplete {
    case Success(_) => log.info("Rest api binding has been successfully done")
    case Failure(thr) =>
      log.error("Rest api binding has been failed", thr)
      sys.exit(1)
  }

}
