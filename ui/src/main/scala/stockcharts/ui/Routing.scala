package stockcharts.ui

import akka.http.scaladsl.server.Directives._
import stockcharts.Config.Simulation.Default

trait Routing {

  val route =
    path("simulate") {
      get {
        parameters('stock,
          'rsiBuy.as[Double] ? Default.rsiBuy,
          'rsiSell.as[Double] ? Default.rsiSell,
          'takeProfit.as[Double] ? Default.takeProfit,
          'stopLoss.as[Double] ? Default.stopLoss) {
          (stock, rsiBuy, rsiSell, takeProfit, stopLoss) =>
            getFromResource("web/index.html")
        }
      }
    } ~ {
      getFromResourceDirectory("web")
    }

}
