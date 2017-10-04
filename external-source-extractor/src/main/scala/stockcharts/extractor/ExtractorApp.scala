package stockcharts.extractor

import java.time.LocalDate

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import org.slf4j.LoggerFactory
import stockcharts.Config.StockSources.Quandl
import stockcharts.extractor.quandl.QuandlClient

case class Price(date: LocalDate,
                 open: Double,
                 high: Double,
                 low: Double,
                 close: Double)

object ExtractorApp extends App {

  val log = LoggerFactory.getLogger(this.getClass)

  val actorSystem = ActorSystem("extractor-app")
  import actorSystem.dispatcher
  implicit val materializer = ActorMaterializer()(actorSystem)

  val quandlClient = new QuandlClient(Quandl.baseUrl, Quandl.apiKey, actorSystem)
  val extractorManager = actorSystem.actorOf(Props(classOf[PricesExtractorManager], quandlClient), "prices-extractor-manager")

  extractorManager ! Extractor.ExtractPricesIfNecessary(Stock(StockId("FBK"), "Facebook"))

}
