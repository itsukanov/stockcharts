package stockcharts.extractor.quandl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import org.json4s._
import org.json4s.native.JsonMethods._
import stockcharts.common.AkkaHttp

import scala.concurrent.{ExecutionContext, Future}

case class StockData(url: String, name: String, fullName: String, description: String)

class QuandlClient(baseUrl: String,
                   apiKey: String,
                   system: ActorSystem)
                  (implicit val ec: ExecutionContext,
                   val m: ActorMaterializer) extends AkkaHttp {

  private implicit val formats = org.json4s.DefaultFormats
  private val httpClient = Http()(system)

  private def getDatasetUrl(stockName: String) =
    s"$baseUrl/$stockName/data.json?api_key=$apiKey"

  def getStockData(stockName: String): Future[String] =
    httpClient
      .singleRequest(HttpRequest(uri = getDatasetUrl(stockName)))
      .flatMap(getContentIf200)
//      .map(parseRepoInfo)

  private def parseRepoInfo(repoInfo: String): StockData =
    parse(repoInfo).camelizeKeys.extract[StockData]

}
