package stockcharts.models

import java.time.LocalDate

import com.typesafe.config.Config

case class Money(cents: Long) {
  def + (that: Money) = Money(cents + that.cents)
}

object Money {
  val zero = Money(0)
}

case class Price(date: LocalDate,
                 open: Money,
                 high: Money,
                 low: Money,
                 close: Money,
                 `type`: String = "Price" // todo delete this field
)

case class StockId(id: String) extends AnyVal

case class Stock(stockId: StockId, uiName: String) {
  val topic = s"prices_${stockId.id}"
}

object Stock {
  def apply(conf: Config): Stock = new Stock(StockId(conf.getString("id")), conf.getString("ui-name"))
}

case class SimulationConf(simulationId: String,
                          stock: Stock,
                          rsiBuy: Double,
                          rsiSell: Double,
                          takeProfit: Money,
                          stopLoss: Money)
