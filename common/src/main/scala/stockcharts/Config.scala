package stockcharts

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._

case class KafkaTopic(name: String, partitions: Int)

object Config {

  private val conf = ConfigFactory.load()

  object Simulation {
    object Default {
      val rsiBuy = conf.getDouble("simulation.default.rsi-buy")
      val rsiSell = conf.getDouble("simulation.default.rsi-sell")
      val takeProfit = conf.getInt("simulation.default.take-profit-cents")
      val stopLoss = conf.getInt("simulation.default.stop-loss-cents")
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