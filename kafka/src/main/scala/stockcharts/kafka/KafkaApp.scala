package stockcharts.kafka

import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.slf4j.LoggerFactory
import stockcharts.Config.{Kafka, ZooKeeper}

import scala.util.{Failure, Success}

object KafkaApp extends App with KafkaSupport {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val config = EmbeddedKafkaConfig(
    kafkaPort = Kafka.port,
    zooKeeperPort = ZooKeeper.port,
    customBrokerProperties = Kafka.properties)

  EmbeddedKafka.start()

  log.info("Topics initialization started")
  initTopics(Kafka.Topics.all) match {
    case Success(_) => log.info("Topics initialization has been successfully done")
    case Failure(thr) =>
      log.error("Topics initialization has been failed:", thr)
      sys.exit(1)
  }

  sys.addShutdownHook {
    EmbeddedKafka.stop()
    log.info("EmbeddedKafka has been stopped")
  }

}
