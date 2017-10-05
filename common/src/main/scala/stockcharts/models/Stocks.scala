package stockcharts.models

import java.time.LocalDate

case class Price(date: LocalDate,
                 open: Double,
                 high: Double,
                 low: Double,
                 close: Double)

case class StockId(id: String) extends AnyVal

case class Stock(stockId: StockId, name: String) {
  val topic = s"prices_${stockId.id}"
}

object Stocks {

}
