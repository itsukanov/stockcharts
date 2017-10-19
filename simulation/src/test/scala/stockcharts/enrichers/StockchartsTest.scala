package stockcharts.enrichers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, Suite}

import concurrent.duration._
import scala.language.postfixOps

trait StockchartsTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  this: Suite =>

  implicit val to = Timeout(3 seconds)
  implicit val as = ActorSystem("as-for-tests")
  implicit val materializer = ActorMaterializer()

  override def afterAll() = {
    as.terminate()
    materializer.shutdown()
  }

}
