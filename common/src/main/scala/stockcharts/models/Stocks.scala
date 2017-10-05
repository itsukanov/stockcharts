package stockcharts.models

case class StockId(id: String) extends AnyVal
case class Stock(stockId: StockId, name: String) {
  val topic = s"prices_${stockId.id}"
}

object Stocks {

}
