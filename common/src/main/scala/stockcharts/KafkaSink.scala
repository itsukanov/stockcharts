package stockcharts

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

object KafkaSink {

  def apply()(implicit system: ActorSystem) = {
    val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers(Config.Kafka.serverUrl)

    Producer.plainSink(producerSettings)
  }

}
