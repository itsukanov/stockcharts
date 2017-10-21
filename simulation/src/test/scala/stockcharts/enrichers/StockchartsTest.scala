package stockcharts.enrichers

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Source
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, Suite}

import concurrent.duration._
import scala.concurrent.Future
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

  def makeIdGen() = {
    var id = 0
    () => {
      id += 1
      id
    }
  }

  override def afterAll() = {
    as.terminate()
    materializer.shutdown()
  }

}
