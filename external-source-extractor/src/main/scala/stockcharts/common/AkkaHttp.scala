package stockcharts.common

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

trait AkkaHttp {

  implicit def m: ActorMaterializer
  implicit def ec: ExecutionContext

  protected val getContentIf200: PartialFunction[HttpResponse, Future[String]] = {
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(_.utf8String)

    case HttpResponse(code, _, entity, _) =>
      entity.dataBytes
        .runFold(ByteString(""))(_ ++ _)
        .map(_.utf8String)
        .map(body => s"Response code='$code', body='$body'")
        .map(errorMessage => throw new RuntimeException(errorMessage))
  }

}
