package stockcharts

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, Suite}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait StockchartsTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  this: Suite =>

  implicit class StreamOps[T](source: Source[T, _]) {
    def toList(implicit m: Materializer): Future[List[T]] =
      source.runFold(List.empty[T]) { case (list, v) => list :+ v}
  }

  implicit val to = Timeout(3 seconds)
  implicit val as = ActorSystem("as-for-tests")
  implicit val materializer = ActorMaterializer()

  val today = LocalDate.now()

  override def afterAll() = {
    as.terminate()
    materializer.shutdown()
  }

}
