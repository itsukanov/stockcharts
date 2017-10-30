package stockcharts.simulation

import akka.actor.ActorSystem
import akka.stream.{FlowShape, Outlet}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Source, Zip}
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
  import GraphDSL.Implicits._

  def simulateTrade(conf: SimulationConf): Flow[Price, UIModel, _] = {
    val accountManager = AccountManager(initialBalance,
      lotSizeChooser = constantSizeLotChooser(lotSize),
      isOrderReadyToClose = takeProfitStopLossChecker(conf.takeProfit, conf.stopLoss))

    Flow.fromGraph(GraphDSL.create() { implicit b =>
      def ticksIn(prices: Outlet[Price], rsi: Outlet[RSIValue]) = {
        val tradeSignals = rsi
          .map(_.value)
          .via(calculateTradeSignals(OverBoughtSoldStrategy(conf.overbought, conf.oversold)))

        val priceAndTradeSignals = b.add(Zip[Price, Option[TradeSignal]]())
        prices        ~> priceAndTradeSignals.in0
        tradeSignals  ~> priceAndTradeSignals.in1
        priceAndTradeSignals.out
          .map { case (price, tradeSignal) => TickIn(price, tradeSignal) }
      }

      val toRSI = IndicatorsSupport.calculating(RSIIndicator(rsiPeriod))

      val priceCachedStage = b.add(Broadcast[Price](3))
      val uiModels = b.add(Merge[UIModel](4))
      val rsiCachedStage = b.add(Broadcast[RSIValue](2))

      priceCachedStage.outlet           ~> toUI[UIPrice]          ~> uiModels  // uiPrices
      priceCachedStage.outlet ~> toRSI  ~> rsiCachedStage.in
      rsiCachedStage.outlet             ~> toUI[UIIndicatorValue] ~> uiModels  // uiIndicatorValues

      val cachedTicksOut = b.add(Broadcast[TickOut](2))
      ticksIn(priceCachedStage.outlet, rsiCachedStage.outlet)
        .via(calculateAccountChanges(accountManager)) ~> cachedTicksOut.in

      cachedTicksOut.outlet                     ~> toUI[UIAccount]      ~> uiModels   // uiAccounts
      cachedTicksOut.outlet.mapConcat(_.events) ~> toUI[UITradeEvent]   ~> uiModels   // uiTradeEvents

      FlowShape(priceCachedStage.in, uiModels.out)
    })
  }

}
