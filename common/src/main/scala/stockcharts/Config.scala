package stockcharts

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._

case class KafkaTopic(name: String, partitions: Int)

object Config {

  private val conf = ConfigFactory.load()

  object StockSources {
    object Quandl {
      val baseUrl = conf.getString("stock-sources.quandl.base-url")
      val apiKey = conf.getString("stock-sources.quandl.api-key")
    }
  }

  object UI {
    val host = conf.getString("ui.host")
    val port = conf.getInt("ui.port")
  }

  object ZooKeeper {
    val host = conf.getString("zoo-keeper.host")
    val port = conf.getInt("zoo-keeper.port")

    val serverUrl = s"$host:$port"
  }

  object Kafka {

    val host = conf.getString("kafka.host")
    val port = conf.getInt("kafka.port")

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
