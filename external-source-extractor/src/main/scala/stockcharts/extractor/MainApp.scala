package stockcharts.extractor

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import stockcharts.extractor.quandl.QuandlClient

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}


object MainApp extends App {

  val cfg = ConfigFactory.load()

  val actorSystem = ActorSystem("data-generator", cfg)
  val executionContext = ExecutionContext.global
  val materializer = ActorMaterializer()(actorSystem)

  val quandlClient = new QuandlClient(
    cfg.getString("data-source.base-url"),
    cfg.getString("data-source.api_key"),
    actorSystem)(executionContext, materializer)

  val fut = quandlClient.getStockData("FB")

  val res = Await.result(fut, Duration.Inf)
  println(res)

}
