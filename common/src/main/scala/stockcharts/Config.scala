package stockcharts

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import stockcharts.models.Stock

case class KafkaTopic(name: String, partitions: Int)

object Config {

  private val conf = ConfigFactory.load()

  object StockSources {
    object Quandl {
      val baseUrl = conf.getString("stock-sources.quandl.base-url")
      val apiKey = conf.getString("stock-sources.quandl.api-key")
    }
  }

  object Stocks {
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

  object Simulation {
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

      val userCommands = KafkaTopic(
        conf.getString("kafka.topics.user-commands.name"),
        conf.getInt("kafka.topics.user-commands.partitions")
      )

      val all = Seq(userCommands)
    }

    val properties = conf.as[Map[String, String]]("kafka.properties")
  }

}
