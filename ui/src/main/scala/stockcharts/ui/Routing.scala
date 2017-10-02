package stockcharts.ui

import akka.http.scaladsl.server.Directives._

trait Routing {

  val route =
    path("simulate") {
      get {
        parameters('stock,
          'rsiBuy.as[Double],
          'rsiSell.as[Double],
          'takeProfit.as[Double],
          'stopLoss.as[Double]) {
          (stock, rsiBuy, rsiSell, takeProfit, stopLoss) =>
            getFromResource("web/index.html")
        }
      }
    } ~ {
      getFromResourceDirectory("web")
    }

}
