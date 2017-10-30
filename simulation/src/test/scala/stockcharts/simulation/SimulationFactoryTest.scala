package stockcharts.simulation

import akka.stream.scaladsl.Source
import stockcharts.Config.Stocks
import stockcharts.StockchartsTest
import stockcharts.models.{Money, Price, SimulationConf}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class SimulationFactoryTest extends StockchartsTest {

  val counter = Iterator.from(0)
  def price(close: Int) = {
    Price(today.plusDays(counter.next()), Money.zero, Money.zero, Money.zero, Money(close))
  }

  "SimulationFactory" should "simulateTrade without exceptions" in {
    val prices = List(42, 44, 45, 41, 37, 39, 42).map(price)
    val simulationConf = SimulationConf("test", Stocks.all.head, 41, 39, Money(2), Money(1))
    val simulationFactory = new SimulationFactory(rsiPeriod = 14, initialBalance = Money(100*100), lotSize = 1)

    noException should be thrownBy {
      val simulationFuture = Source(prices)
        .via(simulationFactory.simulateTrade(simulationConf))
        .toList

      Await.result(simulationFuture, 3 seconds)
    }

  }

}
