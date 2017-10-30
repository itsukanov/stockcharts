package stockcharts

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import stockcharts.models.{Money, Stock}

import scala.concurrent.duration.FiniteDuration

case class KafkaTopic(name: String, partitions: Int, replicationFactor: Int)

object Config {

  private val conf = ConfigFactory.load()

  object StockSources {
    object Quandl {
      val delayBtwCalls = conf.as[FiniteDuration]("stock-sources.quandl.delay-between-calls")
      val baseUrl = conf.getString("stock-sources.quandl.base-url")
      val apiKey = conf.getString("stock-sources.quandl.api-key")
    }
  }

  object Stocks {
    def getById(id: String) = all.find(_.stockId.id == id)

    val all = Seq(
      Stock(conf.getConfig("stocks.facebook")),
      Stock(conf.getConfig("stocks.ibm")),
      Stock(conf.getConfig("stocks.apple")),
      Stock(conf.getConfig("stocks.adobe")),
      Stock(conf.getConfig("stocks.cisco")),
      Stock(conf.getConfig("stocks.ebay")),
      Stock(conf.getConfig("stocks.fox")),
      Stock(conf.getConfig("stocks.google"))
    )
  }

  object UI {
    val host = conf.getString("ui.host")
    val port = conf.getInt("ui.port")
  }

  object RSIStrategyConf {
    val rsiPeriod = conf.getInt("simulation.rsi-strategy.rsi-period")
    val lotSize = conf.getInt("simulation.rsi-strategy.lot-size")
    val initialBalance = Money(cents = conf.getInt("simulation.rsi-strategy.initial-balance-dollars") * 100)
  }

  object SimulationAppConf {
    val host = conf.getString("simulation.host")
    val port = conf.getInt("simulation.port")
  }

  object ZooKeeper {
    val host = conf.getString("zoo-keeper.host")
    val port = conf.getInt("zoo-keeper.port")

    val serverUrl = s"$host:$port"
  }

  object Kafka {

    val host = conf.getString("kafka.host")
    val port = conf.getInt("kafka.port")

    val serverUrl = s"$host:$port"

    object Topics {
      val stockPartitions = conf.getInt("kafka.topics.stock-partitions")
      val stocksReplicationFactor = conf.getInt("kafka.topics.stock-replication-factor")
      val stocks = Stocks.all.map(stock => KafkaTopic(stock.topic, stockPartitions, stocksReplicationFactor))

      val all = stocks
    }

    val properties = conf.as[Map[String, String]]("kafka.properties")
  }

}
