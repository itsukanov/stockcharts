package stockcharts

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import org.slf4j.LoggerFactory

object TestReaderApp extends App {

  // todo delete this

  val logger = LoggerFactory.getLogger(this.getClass)

  implicit val system = ActorSystem("test-app")
  implicit val materializer = ActorMaterializer()

  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers("localhost:6001")
    .withGroupId("group1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val done =
    Consumer.committableSource(consumerSettings, Subscriptions.topics("topic1"))
      .map { msg =>
        logger.info(msg.record.value())
        msg
      }
      .mapAsync(1) { msg =>
        msg.committableOffset.commitScaladsl()
      }
      .runWith(Sink.ignore)

}
