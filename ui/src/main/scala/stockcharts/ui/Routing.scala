package stockcharts.ui

import akka.http.scaladsl.server.Directives._
import stockcharts.Config.Stocks
import stockcharts.json.JsonConverting

trait Routing {

  val route =
    path("simulate") {
      get {
        getFromResource("web/index.html")
      }
    } ~ path("stocks") {
      get {
        complete(JsonConverting.toJson(Stocks.all))
      }
    } ~ {
      getFromResourceDirectory("web")
    }

}
