package stockcharts.kafka

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import stockcharts.Config
import stockcharts.json.JsonConverting
import stockcharts.models.Price

trait OffsetReset {
  def value: String
}

object OffsetReset {
  object Earliest extends OffsetReset {
    val value = "earliest"
  }

  object Latest extends OffsetReset {
    val value = "latest"
  }
}

object KafkaSource {

  def apply(topic: String,
            groupId: String,
            offsetReset: OffsetReset)(implicit system: ActorSystem) = {
    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(Config.Kafka.serverUrl)
      .withGroupId(groupId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset.value)

    Consumer.committableSource(consumerSettings, Subscriptions.topics(topic))
  }

}

object PriceSource {

  def apply(topic: String,
            groupId: String,
            offsetReset: OffsetReset)(implicit system: ActorSystem) =
    KafkaSource(topic, groupId, offsetReset)
      .map(_.record.value())
      .map(JsonConverting.toModel[Price])

}
