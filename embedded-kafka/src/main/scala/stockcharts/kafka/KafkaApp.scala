package stockcharts.kafka

import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}

object KafkaApp extends App {

  implicit val config = EmbeddedKafkaConfig()

  EmbeddedKafka.start()

  sys.addShutdownHook {
    EmbeddedKafka.stop()
    println("stopped")
  }

}
