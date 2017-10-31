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
                 close: Money)

case class StockId(id: String) extends AnyVal

case class Stock(stockId: StockId, uiName: String) {
  val topic = s"prices_${stockId.id}"
}

object Stock {
  def apply(conf: Config): Stock = new Stock(StockId(conf.getString("id")), conf.getString("ui-name"))
}

case class SimulationConf(simulationId: String,
                          stock: Stock,
                          overbought: Double,
                          oversold: Double,
                          takeProfit: Money,
                          stopLoss: Money) {
  require(oversold > 0,           "oversold must be positive double")
  require(takeProfit.cents > 0,   "takeProfit must be positive double")
  require(stopLoss.cents > 0,     "stopLoss must be positive double")
  require(overbought > 0,         "overbought must be positive double")
  require(overbought < 100,       "overbought must be < 100")
  require(overbought > oversold,  "overbought must be > oversold")
}
