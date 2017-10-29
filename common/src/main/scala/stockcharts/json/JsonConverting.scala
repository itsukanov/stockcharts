package stockcharts.json

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.stream.scaladsl.Flow
import org.json4s._
import org.json4s.native.JsonMethods._

class LocalDateSerializer extends CustomSerializer[LocalDate](format => (
  { case JString(s) => LocalDate.parse(s) },
  { case x: LocalDate => JString(x.format(DateTimeFormatter.ISO_DATE)) }
  ))

object JsonConverting {

  implicit val formats = org.json4s.DefaultFormats + new LocalDateSerializer

  def toJson(a: Any) = compact(render(Extraction.decompose(a))) // type Any came from `Extraction.decompose` todo find another api or remove json4s library

  val toJsonFlow = Flow.fromFunction(toJson)

  def toModel[T: Manifest](json: String) = parse(json)
    .extract[T]

}
