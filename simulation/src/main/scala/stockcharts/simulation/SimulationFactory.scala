package stockcharts.simulation

import akka.actor.ActorSystem
import akka.stream.SourceShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Source}
import akka.util.Timeout
import stockcharts.json.JsonConverting
import stockcharts.kafka.{KafkaSource, OffsetReset}
import stockcharts.models.{Money, Price, SimulationConf}
import stockcharts.simulation.enrichers.indicators.RSIIndicator
import stockcharts.simulation.uisupport.UIModel

object SimulationFactory {
  import stockcharts.simulation.enrichers.PriceStreamsSupport._
  import stockcharts.simulation.uisupport.UIConverters._

  def simulateTrade(rsiPeriod: Int, initialBalance: Money, lotSize: Int)
                   (conf: SimulationConf)
                   (implicit system: ActorSystem, to: Timeout): Source[UIModel, _] =
    Source.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val prices = KafkaSource(topic = conf.stock.topic, groupId = conf.simulationId, OffsetReset.Earliest)
        .map(_.record.value())
        .map(JsonConverting.toModel[Price])

      val toRSI = Flow[Price].map(identity).calculate(RSIIndicator(rsiPeriod)) // try to make `.calculate` as a separate Flow

      val priceCache = b.add(Broadcast[Price](2))
      val merge = b.add(Merge[UIModel](2))

      prices ~> priceCache.in

      priceCache.out(0).map(x => toUIModel(x)) ~> merge.in(0)
      priceCache.out(1) ~> toRSI.map(x => toUIModel(x)) ~> merge.in(1)

      SourceShape(merge.out)
    })
}
