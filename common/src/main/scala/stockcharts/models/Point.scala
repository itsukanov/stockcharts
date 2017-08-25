package stockcharts.models

import java.time.LocalDateTime

case class Point(date: LocalDateTime, bid: Long, ask: Long)
