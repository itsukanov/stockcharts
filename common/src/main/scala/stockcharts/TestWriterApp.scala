package stockcharts

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

object TestWriterApp extends App {

  // todo delete this

  val logger = LoggerFactory.getLogger(this.getClass)

  implicit val system = ActorSystem("test-app")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers("localhost:6001")

  val done = Source(1 to 10)
    .map(_.toString)
    .map { elem =>
      new ProducerRecord[Array[Byte], String](Config.Kafka.Topics.userCommands.name, 1, null, elem)
    }
    .runWith(Producer.plainSink(producerSettings))

  done.onComplete {
    case Success(_) => logger.info("writing done")
    case Failure(thr) => logger.error("fail", thr)
  }

}
