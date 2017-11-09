package stockcharts.ui

import akka.http.scaladsl.server.Directives._
import stockcharts.Config.Stocks
import stockcharts.json.JsonConverting

class Routing(simulationUrl: String) {

  val route =
    path("simulate") {
      get {
        getFromResource("web/index.html")
      }
    } ~ path("simulation-url") {
      get {
        complete(simulationUrl)
      }
    } ~ path("stocks") {
      get {
        complete(JsonConverting.toJson(Stocks.all))
      }
    } ~ {
      getFromResourceDirectory("web")
    }

}
