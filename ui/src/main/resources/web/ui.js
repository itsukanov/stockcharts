var logSystemEventsEnabled = true;
var logDataEnabled = false;

var nbrOfBarsOnChart = 91;
var showNewBarEveryMs = 250;

var id2ParamName = {
    "stock-dropdown": "stock",
    "overbought": "overbought",
    "oversold": "oversold",
    "takeProfit": "takeProfit",
    "stopLoss": "stopLoss"
}
var inputIds = Object.keys(id2ParamName);

var serverUri = "ws://localhost:8081/simulate";
var stock2Id = new Map();
var websocket;

var allDataFromServer = [];

var priceData = [];
var indicatorData = [];
var balanceData = [];
var equityData = [];
var stockEvents = [];
var trendLines = [];
var indicatorLevels = [];

$(function(){
     initStockList();
     initInputs();
     $("#start-btn").click(startBtnHandler);
     chart = createChart();
})

function initSkipAnimationBtn() {
    $("#start-btn").text("Skip animation").removeClass("btn-primary").addClass("btn-warning");
    $("#start-btn").off().click(function () { skipAnimation = true});
}

function initStartBtn() {
    $("#start-btn").text("Start simulation").removeClass("btn-warning").addClass("btn-primary");
    $("#start-btn").off().click(startBtnHandler);
}

function drawIndicatorLevels() {
    var simulationConf = getSimulationConf();
    indicatorLevels.push({
      "value": simulationConf.overbought,
      "lineColor": "#db4c3c"
    }, {
      "value": simulationConf.oversold,
      "lineColor": "#db4c3c"
    });
}

var skipAnimation = false;
var isChartWithOldData = false;
function startBtnHandler() {
    initSkipAnimationBtn();
    startProgressBarUpdating();

    if (isChartWithOldData) {
      clearAllDataFromServer();
    } else {
      isChartWithOldData = true;
    }
    drawIndicatorLevels();

    function sendSimulationConf() {
        var simulationConf = getSimulationConf();
        websocket.send(JSON.stringify(simulationConf));
        log("Request to start simulation has been sent");
    }

    saveConfInQParams();

    if (websocket == undefined) {
        websocket = initWebSocket(serverUri);

        var waitingSocketConnection = setInterval(function () {
            if (websocket.readyState == websocket.OPEN) {
                sendSimulationConf();
                clearInterval(waitingSocketConnection);
            }
        }, 200);
    } else {
        sendSimulationConf();
    }

    startChartUpdating();
}

function initStockList() {
    fetch("/stocks").then(function(response) {
      return response.json();
    }).then(function(stocks) {
      var stockList = $("#stock-list");
      stocks.forEach(function(stock) {
        stock2Id.set(stock.uiName, stock.stockId);
        stockList.append('<a class="dropdown-item" href="#">' + stock.uiName + '</a>')
      });

      $(".dropdown-item").click(function(){
        var selText = $(this).text();
        $(this).parents('.dropdown').find('.dropdown-toggle').html(selText);
      });
    })
}

function saveConfInQParams() {
    function isNotBlank(str) {
       return !(str == "" || str == null);
    }

    var newSimulationParams = inputIds.filter(function (id) {
        return isNotBlank(getValue(id));
    }).map(function (id) {
        return id2ParamName[id] + "=" + getValue(id);
    }).join("&");

    window.history.pushState('page', 'Title', "/simulate?" + newSimulationParams);
}

function getValue(id) {
      if (id.includes('dropdown')) {
          return $("#" + id).text().trim();
      } else {
          return $("#" + id).val().trim();
      }
    }

function getSimulationConf() {
    return {
        stock: stock2Id.get(getValue("stock-dropdown")),
        overbought: parseFloat(getValue("overbought")),
        oversold: parseFloat(getValue("oversold")),
        takeProfit: parseFloat(getValue("takeProfit")),
        stopLoss: parseFloat(getValue("stopLoss"))
    }
}

var progressBarUpdating;
function startProgressBarUpdating() {
    $(".progress-bar").show();

    progressBarUpdating = setInterval(function () {
        if (receivedFromServerCount > 0) {
            var percentDone = (priceData.length / receivedFromServerCount * 100).toFixed(2);
            var percents = percentDone + "%";
            $(".progress-bar").css("width", percents).text(percents);
        }
    }, 250);
}

function stopProgressBarUpdating() {
    clearInterval(progressBarUpdating);
    $(".progress-bar").hide().css("width", "100%").text("Loading..");
}

function createChart() {
        return AmCharts.makeChart("chartdiv", {
              "type": "stock",
              "theme": "dark",
              "addClassNames": true,
              "glueToTheEnd": true,
              "listeners": [{
                "event": "rendered",
                "method": function() { chartWasRendered = true; }
              }],

              "dataSets": [ {
                "title": "",
                "fieldMappings": [ {
                  "fromField": "open",
                  "toField": "open"
                }, {
                  "fromField": "high",
                  "toField": "high"
                }, {
                  "fromField": "low",
                  "toField": "low"
                }, {
                  "fromField": "close",
                  "toField": "close"
                } ],
                "categoryField": "date",
                "dataProvider": priceData,
                "stockEvents": stockEvents
              },
               {
               "title": "Indicator",
               "fieldMappings": [ {
                 "fromField": "indicatorValue",
                 "toField": "indicatorValue"
               } ],
               "compared": true,
               "categoryField": "date",
               "dataProvider": indicatorData
               },
               {
                "title": "Balance",
                "fieldMappings": [ {
                  "fromField": "balance",
                  "toField": "balance"
                }],
                "compared": true,
                "categoryField": "date",
                "dataProvider": balanceData
              },{
                "title": "Equity",
                "fieldMappings": [{
                    "fromField": "equity",
                    "toField": "equity"
                  }],
                "compared": true,
                "categoryField": "date",
                "dataProvider": equityData
              }],
              "dataDateFormat": "YYYY-MM-DD",

              "panels": [ {
                  "title": "Prices",
                  "percentHeight": 70,
                  "recalculateToPercents": "never",

                  "stockGraphs": [ {
                    "type": "candlestick",
                    "id": "g1",
                    "openField": "open",
                    "closeField": "close",
                    "highField": "high",
                    "lowField": "low",
                    "valueField": "close",
                    "lineColor": "#fff",
                    "fillColors": "#fff",
                    "negativeLineColor": "#db4c3c",
                    "negativeFillColors": "#db4c3c",
                    "fillAlphas": 1,
                    "comparable": true,
                    "comparedGraphLineThickness": 2,
                    "columnWidth": 0.7,
                    "useDataSetColors": false,
                    "showBalloon": false
                  } ],
                  "trendLines": trendLines,

                  "stockLegend": {
                   "valueTextRegular": "Open: [[open]]; High: [[high]]; Low: [[low]]; Close [[close]]",
                    "markerType": "none",
                    "markerSize": 0,
                    "switchable": false
                  }
                },

                {
                  "title": "RSI",
                  "percentHeight": 30,
                  "marginTop": 1,
                  "columnWidth": 0.6,
                  "recalculateToPercents": "never",
                  "showCategoryAxis": false,

                  "guides": indicatorLevels,

                  "stockGraphs": [ {
                    "type": "line",
                    "id": "g2",
                    "valueField": "indicatorValue",
                    "showBalloon": false,
                    "comparable": true,
                    "lineColor": "#fff",
                    "useDataSetColors": false
                  } ],

                 "stockLegend": {
                    "markerType": "none",
                    "markerSize": 0,
                    "switchable": false,
                    "labelText": "",
                    "periodValueTextRegular": "[[value.indicatorValue]]"
                  },

                  "valueAxes": [ {
                    "usePrefixes": true,
                    "autoGridCount": false,
                    "maximum": 100,
                    "minimum": 0,
                    "precision": 0,
                    "gridCount": 5
                  } ]
                },

                {
                  "title": "Account",
                  "percentHeight": 30,
                  "marginTop": 1,
                  "columnWidth": 0.6,
                  "recalculateToPercents": "never",
                  "showCategoryAxis": false,

                  "stockGraphs": [ {
                    "type": "line",
                    "id": "g3",
                    "valueField": "balance",
                    "comparable": true,
                    "showBalloon": false,
                    "lineThickness": 3
                  },{
                    "type": "line",
                    "id": "g4",
                    "valueField": "equity",
                    "showBalloon": false,
                    "comparable": true,
                    "lineColor": "red"
                  } ],

                 "stockLegend": {
                    "switchable": false
                  }
                }
              ],

              "panelsSettings": {
                "plotAreaFillColors": "#333",
                "plotAreaFillAlphas": 1,
                "marginLeft": 60,
                "marginTop": 5,
                "marginBottom": 5
              },

              "chartScrollbarSettings": {
                "graph": "g1",
                "graphType": "line",
                "resizeEnabled": true,
                "usePeriod": "DD",
                "backgroundColor": "#333",
                "graphFillColor": "#666",
                "graphFillAlpha": 0.5,
                "gridColor": "#555",
                "gridAlpha": 11,
                "selectedBackgroundColor": "#444",
                "selectedGraphFillAlpha": 1
              },

              "categoryAxesSettings": {
                "minPeriod": "DD",
                "maxSeries": 0,
                "gridColor": "#555",
                "gridAlpha": 1
              },

              "valueAxesSettings": {
                "gridColor": "#555",
                "gridAlpha": 1,
                "inside": false,
                "showLastLabel": true
              },

              "chartCursorSettings": {
                "pan": true,
                "valueLineEnabled": true,
                "valueLineBalloonEnabled": true,
                "valueBalloonsEnabled": true
              },

              "balloon": {
                "textAlign": "left",
                "offsetY": 10
              }
            } );
        }

var order1 = {
    id: 42,
    openDate: "2017-09-25",
    openPrice: 42,
    type: "buy"
};

var order2 = {
    id: 42,
    openDate: "2017-09-25",
    openPrice: 42,
    closePrice: 43,
    closeDate: "2017-09-27",
    balanceChange: "+12",
    type: "buy"
};

var order3 = {
    id: 43,
    openDate: "2017-09-21",
    openPrice: 44,
    type: "sell"
};

var order4 = {
    id: 43,
    openDate: "2017-09-21",
    closeDate: "2017-09-23",
    openPrice: 44,
    closePrice: 45,
    balanceChange: "-23",
    type: "sell"
};

function formatProfit(balanceChange) {
    var formatted = balanceChange.toFixed(2);
    if (balanceChange >= 0) return "+$" + formatted;
    else return "-$" + ( - formatted);
}

function openBuyEvent(order) {
    return {
        "date": order.openDate,
        "type": "arrowUp",
        "backgroundColor": "blue",
        "rollOverColor": "blue",
        "graph": "g1",
        "showAt": "open",
        "description": "Opened 'buy' order#" + order.id
      }
}

function closeBuyEvent(order) {
    return {
        "date": order.closeDate,
        "type": "arrowDown",
        "backgroundColor": "red",
        "rollOverColor": "red",
        "graph": "g1",
        "showAt": "close",
        "description": "Closed 'buy' order#" + order.id + ". Profit " + formatProfit(order.balanceChange)
      }
}

function openSellEvent(order) {
    return {
        "date": order.openDate,
        "type": "arrowDown",
        "backgroundColor": "red",
        "rollOverColor": "red",
        "graph": "g1",
        "showAt": "open",
        "description": "Opened 'sell' order#" + order.id
      }
}

function closeSellEvent(order) {
    return {
        "date": order.closeDate,
        "type": "arrowUp",
        "backgroundColor": "blue",
        "rollOverColor": "blue",
        "graph": "g1",
        "showAt": "close",
        "description": "Closed 'sell' order#" + order.id + ". Profit " + formatProfit(order.balanceChange)
      }
}

function clearAllDataFromServer() {
    allDataFromServer = [];
    priceData = [];
    indicatorData = [];
    balanceData = [];
    equityData = [];
    stockEvents = [];
    trendLines = [];
    indicatorLevels = [];

    AmCharts.clear();
    chart = createChart();
    chartWasRendered = false;
    receivedFromServerCount = 0;
}

function addTradeEvent(order) {
    var event;
    if (order.closeDate) {
        if (order.orderType == "buy") {
          event = closeBuyEvent(order);
        } else {
          event = closeSellEvent(order);
        }
        addOrderLine(order);
    } else {
        if (order.orderType == "buy") {
          event = openBuyEvent(order);
        } else {
          event = openSellEvent(order);
        }
    }
    stockEvents.push(event);
}

var lastOrderLineId = 0;
function addOrderLine(order) {
    var initialDate = new Date(order.openDate);
    initialDate.setHours(12);

    var finalDate = new Date(order.closeDate);
    finalDate.setHours(12);

    lastOrderLineId = lastOrderLineId + 1;

    trendLines.push({
        "id": "t1",
        "initialDate": initialDate,
        "finalDate": finalDate,
        "initialValue": order.openPrice,
        "finalValue": order.closePrice,
        "lineAlpha": 1,
        "lineThickness": 2,
        "lineColor": "#e1d165"
      });
}

function initWebSocket(wsUri) {
  var ws = new WebSocket(wsUri);
  ws.onopen = onConnect;
  ws.onclose = onClose;
  ws.onerror = onError;
  ws.onmessage = saveData;
  return ws;
}

var receivedFromServerCount = 0;
function saveData(wsEvent) {
    logData("Received data from server:\n" + wsEvent.data);
    allDataFromServer.push(wsEvent.data);
    receivedFromServerCount = receivedFromServerCount + 1;
}

var chartWasRendered = false;
function startChartUpdating() {
    var chartUpdating = setInterval(function() {
        do {
            if (allDataFromServer.length > 0) {
                processWsEvent(chartUpdating);
            }
        } while ((priceData.length < nbrOfBarsOnChart || (skipAnimation && chartWasRendered)) && allDataFromServer.length > 0)

        if (priceData.length >= nbrOfBarsOnChart) {
            chart.validateData(); //call to redraw the chart with new data
        }
    }, showNewBarEveryMs);
}

function processWsEvent(chartUpdating) {
    var wsEvent = allDataFromServer.shift();
    logData("Processing ws event:\n" + wsEvent);

    var newData = JSON.parse(wsEvent);
    switch (newData.type) {
      case 'Price':
        priceData.push(newData);
        break;
      case 'IndicatorValue':
        newData.indicatorValue = newData.indicatorValue.toFixed(2);
        indicatorData.push(newData);
        break;
      case 'TradeEvent':
        addTradeEvent(newData);
        break;
      case 'Account':
        newData.equity = undefined;
        balanceData.push(newData);

        var copy = JSON.parse(wsEvent);
        copy.balance = undefined;
        equityData.push(copy);
        break;
      case 'Simulation done':
        clearInterval(chartUpdating);
        stopProgressBarUpdating();
        log("Simulation done. Chart updating stopped");
        initStartBtn();
        skipAnimation = false;
        break;
    }
}

function onConnect(wsEvent) {
  log("Server connection successful. Listening for data now.");
}

function onError(wsEvent) {
  log("ERROR:" + wsEvent);
}

function onClose(wsEvent) {
  log("Server connection closed");
  websocket = null;
}

function log(msg) {
  if (logSystemEventsEnabled) {
    console.log(msg);
  }
}

function logData(data) {
  if (logDataEnabled) {
    console.log(data);
  }
}

function initInputs() {
  inputIds.map(function (id) {
    setInput(id2ParamName[id], id);
  });
}

function setInput(qParam, inputId) {
  function getQueryParam(param) {
    var url = new URL(window.location.href);
    return url.searchParams.get(param);
  }

  if (inputId.includes('dropdown')) {
    $("#" + inputId).parents('.dropdown').find('.dropdown-toggle').html(getQueryParam(qParam));
  } else {
    $("#" + inputId).val(getQueryParam(qParam));
  }
}
