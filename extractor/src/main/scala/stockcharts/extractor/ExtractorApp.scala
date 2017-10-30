package stockcharts.extractor

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.slf4j.LoggerFactory
import stockcharts.Config
import stockcharts.Config.StockSources.Quandl
import stockcharts.extractor.quandl.ThrottledQuandlClient

import scala.concurrent.ExecutionContext

object ExtractorApp extends App {

  val log = LoggerFactory.getLogger(this.getClass)

  val system = ActorSystem("extractor-app")
  implicit val materializer = ActorMaterializer()(system)


  val quandlClient = new ThrottledQuandlClient(Quandl.baseUrl, Quandl.apiKey, Quandl.delayBtwCalls, system)(
    ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)) , materializer)
  val extractorManager = system.actorOf(PricesExtractorManager.props(quandlClient), "prices-extractor-manager")

  Config.Stocks.all.foreach { stock =>
    extractorManager ! PricesExtractor.ExtractPricesIfNecessary(stock)
  }

}
