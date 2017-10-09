package stockcharts.simulation

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import stockcharts.kafka.{KafkaSource, OffsetReset}
import stockcharts.models.SimulationConf

object SimulationFactory {

  def simulateTrade(conf: SimulationConf)(implicit system: ActorSystem): Source[String, _] =
        KafkaSource(topic = conf.stock.topic, groupId = conf.simulationId, OffsetReset.Earliest)
          .map(_.record.value())
}
