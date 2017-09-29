package stockcharts

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._

case class KafkaTopic(name: String, partitions: Int)

object Config {

  private val conf = ConfigFactory.load()

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
