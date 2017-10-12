package stockcharts.models

import java.time.LocalDate

import com.typesafe.config.Config

case class Price(`type`: String = "Price", // todo delete this field
                  date: LocalDate,
                 open: Double,
                 high: Double,
                 low: Double,
                 close: Double)

case class StockId(id: String) extends AnyVal

case class Stock(stockId: StockId, name: String) {
  val topic = s"prices_${stockId.id}"
}

object Stock {
  def apply(conf: Config): Stock = new Stock(StockId(conf.getString("id")), conf.getString("name"))
}

case class SimulationConf(simulationId: String,
                          stock: Stock,
                          rsiBuy: Double,
                          rsiSell: Double,
                          takeProfit: Double,
                          stopLoss: Double)
