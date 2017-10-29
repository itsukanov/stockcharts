package stockcharts.extractor

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.slf4j.LoggerFactory
import stockcharts.Config
import stockcharts.Config.StockSources.Quandl
import stockcharts.extractor.quandl.QuandlClient

object ExtractorApp extends App {

  val log = LoggerFactory.getLogger(this.getClass)

  val system = ActorSystem("extractor-app")
  implicit val materializer = ActorMaterializer()(system)

  val quandlClient = new QuandlClient(Quandl.baseUrl, Quandl.apiKey, system)(system.dispatcher, materializer) // todo change actorSystem.dispatcher to another context
  val extractorManager = system.actorOf(PricesExtractorManager.props(quandlClient), "prices-extractor-manager")

  Config.Stocks.all.foreach { stock =>
    Thread.sleep(1200) // todo replace with SINGLE throttling inside QuandlClient
    extractorManager ! PricesExtractor.ExtractPricesIfNecessary(stock)
  }

}
