package stockcharts.simulation

import akka.http.scaladsl.server.Directives._

trait Routing {

  val route =
    path("simulate") {
      get {
        complete("hello Bobbie!")
      }
    }

}
