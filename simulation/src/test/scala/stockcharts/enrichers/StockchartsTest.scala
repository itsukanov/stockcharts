package stockcharts.enrichers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, Suite}

trait StockchartsTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  this: Suite =>

  implicit val as = ActorSystem("as-for-tests")
  implicit val materializer = ActorMaterializer()

  override def afterAll() = {
    as.terminate()
    materializer.shutdown()
  }

}
