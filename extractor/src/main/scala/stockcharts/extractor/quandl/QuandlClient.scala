package stockcharts.extractor.quandl

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import org.json4s._
import org.json4s.native.JsonMethods._
import stockcharts.common.AkkaHttp
import stockcharts.models.{Money, Price}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

case class QuandlPlainResponse(data: List[List[String]])

class QuandlClient(baseUrl: String,
                   apiKey: String,
                   system: ActorSystem)
                  (implicit val ec: ExecutionContext,
                   val m: ActorMaterializer) extends AkkaHttp {

  private implicit val formats = org.json4s.DefaultFormats
  private val httpClient = Http()(system)

  private def getDatasetUrl(stockName: String) =
    s"$baseUrl/$stockName/data.json?api_key=$apiKey&order=asc"

  def getStockPrices(stock: String): Future[List[Price]] =
    httpClient
      .singleRequest(HttpRequest(uri = getDatasetUrl(stock)))
      .flatMap(getContentIf200)
      .map(parsePrices)

  private val dateInx = 0
  private val openInx = 1
  private val highInx = 2
  private val lowInx = 3
  private val closeInx = 4

  private def row2Price(row: List[String]) = Price(
    date = LocalDate.parse(row(dateInx)),
    open = Money(row(openInx).toDouble * 100 toLong),
    high = Money(row(highInx).toDouble * 100 toLong),
    low = Money(row(lowInx).toDouble * 100 toLong),
    close = Money(row(closeInx).toDouble * 100 toLong)
  )

  private def parsePrices(json: String): List[Price] =
    parse(json)
      .\("dataset_data")
      .extract[QuandlPlainResponse]
      .data
      .map(row2Price)

}
