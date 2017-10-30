package stockcharts.simulation

import akka.actor.ActorSystem
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Zip}
import akka.util.Timeout
import stockcharts.models.{Money, Price, SimulationConf}
import stockcharts.simulation.enrichers.indicators.{IndicatorsSupport, RSIIndicator, RSIValue}
import stockcharts.simulation.enrichers.tradeevents.SimulationSupport._
import stockcharts.simulation.enrichers.tradeevents.{AccountManager, TickIn, TickOut}
import stockcharts.simulation.enrichers.tradesignals.{OverBoughtSoldStrategy, TradeSignal}
import stockcharts.simulation.uisupport.{UIAccount, UIIndicatorValue, UIModel, UIPrice, UITradeEvent}

class SimulationFactory(rsiPeriod: Int,
                        initialBalance: Money,
                        lotSize: Int)
                       (implicit system: ActorSystem, to: Timeout) {
  import stockcharts.simulation.enrichers.tradesignals.TradeSignalsSupport.calculateTradeSignals
  import stockcharts.simulation.uisupport.UIConverters._

  def simulateTrade(conf: SimulationConf): Flow[Price, UIModel, _] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val toRSI = IndicatorsSupport.calculating(RSIIndicator(rsiPeriod))

      val priceCachedStage = b.add(Broadcast[Price](3))
      val uiModels = b.add(Merge[UIModel](4))
      val rsiCachedStage = b.add(Broadcast[RSIValue](2))

      priceCachedStage.out(0)           ~> toUI[UIPrice]          ~> uiModels.in(0)  // uiPrices

      priceCachedStage.out(1) ~> toRSI  ~> rsiCachedStage.in
      rsiCachedStage.out(0)             ~> toUI[UIIndicatorValue] ~> uiModels.in(1)  // uiIndicatorValues

      val priceAndTradeSignals = b.add(Zip[Price, Option[TradeSignal]]())
      priceCachedStage.out(2) ~> priceAndTradeSignals.in0

      val tradeSignals = rsiCachedStage.out(1)
        .map(_.value)
        .via(calculateTradeSignals(OverBoughtSoldStrategy(conf.overbought, conf.oversold)))
      tradeSignals ~> priceAndTradeSignals.in1

      val accountManager = AccountManager(initialBalance,
        lotSizeChooser = constantSizeLotChooser(lotSize),
        isOrderReadyToClose = takeProfitStopLossChecker(conf.takeProfit, conf.stopLoss))

      val cachedTicksOut = b.add(Broadcast[TickOut](2))
      priceAndTradeSignals.out
        .map { case (price, tradeSignal) => TickIn(price, tradeSignal) }
        .via(calculateAccountChanges(accountManager)) ~> cachedTicksOut.in

      cachedTicksOut.out(0) ~> toUI[UIAccount] ~> uiModels // uiAccounts
      cachedTicksOut.out(1).mapConcat(_.events) ~> toUI[UITradeEvent] ~> uiModels // uiTradeEvents

      FlowShape(priceCachedStage.in, uiModels.out)
    })
}
