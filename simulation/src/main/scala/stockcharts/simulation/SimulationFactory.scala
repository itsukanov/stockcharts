package stockcharts.simulation

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.Timeout
import stockcharts.json.JsonConverting
import stockcharts.kafka.{KafkaSource, OffsetReset}
import stockcharts.models.{Money, Price, SimulationConf}
import stockcharts.simulation.enrichers.indicators.RSIIndicator
import stockcharts.simulation.enrichers.tradeevents.{AccountManager, TickIn}
import stockcharts.simulation.enrichers.tradesignals.OverBoughtSoldStrategy
import stockcharts.simulation.uisupport.UIModel

object SimulationFactory {
  import stockcharts.simulation.enrichers.NumericStreamsSupport._
  import stockcharts.simulation.enrichers.PriceStreamsSupport._
  import stockcharts.simulation.enrichers.tradesignals.SimulationSupport._
  import stockcharts.simulation.uisupport.UIConverters._

  def simulateTrade(rsiPeriod: Int, initialBalance: Money, lotSize: Int)
                   (conf: SimulationConf)
                   (implicit system: ActorSystem, to: Timeout): Source[UIModel, _] = {
    val plainRows = KafkaSource(topic = conf.stock.topic, groupId = conf.simulationId, OffsetReset.Earliest)
          .map(_.record.value())

    val prices = plainRows.map(JsonConverting.toModel[Price])
    val rsi = prices.calculate(RSIIndicator(rsiPeriod))
    val tradeSignals = rsi
      .map(_.value)
      .calculateTradeSignals(OverBoughtSoldStrategy(conf.rsiSell, conf.rsiBuy))

    val ticksIn = prices zip tradeSignals map { case (price, signal) => TickIn(price, signal) }
    val accountChanges = calculateAccountChanges(ticksIn, AccountManager(
      initialBalance,
      constantSizeLotChooser(lotSize),
      takeProfitStopLossChecker(conf.takeProfit, conf.stopLoss))
    )

    val uiPrices = prices.map(x => toUIModel(x))
    val uiIndicator = rsi.map(x => toUIModel(x))

    val uiPrices2 = KafkaSource(topic = conf.stock.topic, groupId = conf.simulationId + "asd", OffsetReset.Earliest) // todo find how to use plainRows instead
      .map(_.record.value())
      .map(JsonConverting.toModel[Price])
      .map(x => toUIModel(x))

    uiPrices2 merge uiIndicator
  }
}

