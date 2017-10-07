package stockcharts.kafka

import akka.stream.scaladsl.Flow
import org.apache.kafka.clients.producer.ProducerRecord

object KafkaRecords {

  private def toProducerRecord(topic: String)(msg: String) =
    new ProducerRecord[Array[Byte], String](topic, null, msg)

  def toKafkaRecordFlow(topic: String) = Flow.fromFunction(toProducerRecord(topic))

}
