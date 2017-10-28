package stockcharts.simulation.uisupport

import java.time.LocalDate

trait UIModel {
  def `type`: String
}

case class UIPrice(date: LocalDate,
                   open: Double,
                   high: Double,
                   low: Double,
                   close: Double,
                   `type`: String = "Price") extends UIModel

case class UIIndicatorValue(date: LocalDate,
                            indicatorValue: Double,
                            `type`: String = "IndicatorValue") extends  UIModel

case class UIAccount(date: LocalDate,
                     balance: Double,
                     equity: Double,
                     `type`: String = "Account") extends UIModel

trait UITradeEvent extends UIModel
object UITradeEvent {

  case class UIOrderOpened(id: Long,
                           openDate: LocalDate,
                           orderType: String,
                           `type`: String = "TradeEvent") extends UITradeEvent

  case class UIOrderClosed(id: Long,
                           openDate: LocalDate,
                           closeDate: LocalDate,
                           openPrice: Double,
                           closePrice: Double,
                           balanceChange: Double,
                           orderType: String,
                           `type`: String = "TradeEvent") extends UITradeEvent
}
