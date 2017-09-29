package stockcharts.ui

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object UIApp extends App with Routing {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val binding = Http().bindAndHandle(route, "localhost", 8080)
  binding.onComplete {
    case Success(_) => log.info("Rest api binding has been successfully done")
    case Failure(thr) =>
      log.error("Rest api binding has been failed", thr)
      sys.exit(1)
  }

}
