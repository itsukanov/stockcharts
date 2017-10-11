var debugEnabled = true;

var nbrOfBarsOnChart = 10;
var showNewBarEveryMs = 101;
var stockName = "Facebook";

var id2ParamName = {
    "stock-dropdown": "stock",
    "rsiBuy": "rsiBuy",
    "rsiSell": "rsiSell",
    "takeProfit": "takeProfit",
    "stopLoss": "stopLoss"
}
var inputIds = Object.keys(id2ParamName);

var stock2Id = {
 "Facebook": "FB"
}

var serverUri = "ws://localhost:8081/simulate";
var websocket;
var chartUpdating;

var allDataFromServer = [];

var priceData = [];
var indicatorData = [];
var balanceData = [];
var equityData = [];
var stockEvents = [];
var trendLines = [];

$(function(){
     initInputs();

     $(".dropdown-item").click(function(){
         var selText = $(this).text();
         $(this).parents('.dropdown').find('.dropdown-toggle').html(selText);
     });

     $("#start-btn").click(function(){
        function getValue(id) {
          if (id.includes('dropdown')) {
              return $("#" + id).text().trim();
          } else {
              return $("#" + id).val().trim();
          }
        }

//        clearAllDataFromServer();
        var simulationConf = {
            stock: stock2Id[getValue("stock-dropdown")],
            rsiBuy: parseFloat(getValue("rsiBuy")),
            rsiSell: parseFloat(getValue("rsiSell")),
            takeProfit: parseFloat(getValue("takeProfit")),
            stopLoss: parseFloat(getValue("stopLoss"))
        }

        if (websocket == undefined) {
            websocket = initWebSocket(serverUri);
        }

        websocket.send(JSON.stringify(simulationConf));
        startChartUpdating();
     });

     chart = AmCharts.makeChart("chartdiv", {
              "type": "stock",
              "theme": "dark",
              "addClassNames": true,
              "glueToTheEnd": true,

              "dataSets": [ {
                "title": stockName,
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
                  "title": "Value",
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
})

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
        "description": "Closed 'buy' order#" + order.id + ". Balance change " + order.balanceChange
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
        "description": "Closed 'sell' order#" + order.id + ". Balance change " + order.balanceChange
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

    chart.validateData();
}

function addOrderEvent(order) {
    var event;
    if (order.closeDate) {
        if (order.type == "buy") {
          event = closeBuyEvent(order);
        } else {
          event = closeSellEvent(order);
        }
        addOrderLine(order);
    } else {
        if (order.type == "buy") {
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
        "id": "tl" + lastOrderLineId,
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

function saveData(wsEvent) {
    log("Received: " + wsEvent.data);
    allDataFromServer.push(wsEvent)
}

function startChartUpdating() {
    chartUpdating = setInterval(function() {
        do {
            if (allDataFromServer.length > 0) {
                processWsEvent();
            }
        } while ((priceData.length < nbrOfBarsOnChart) && (allDataFromServer.length > 0))

        if (priceData.length >= nbrOfBarsOnChart) {
            chart.validateData(); //call to redraw the chart with new data
        }
    }, showNewBarEveryMs);
}

function processWsEvent() {
    var wsEvent = allDataFromServer.shift();
    var newData = JSON.parse(wsEvent.data);
    log("Processing ws event:");
    log(newData);

    switch (newData.type) {
      case 'Price':
        priceData.push(newData);
        break;
      case 'IndicatorValue':
        indicatorData.push(newData);
        break;
      case 'Balance':
        newData.equity = undefined;
        balanceData.push(newData);

        var copy = JSON.parse(wsEvent.data);
        copy.balance = undefined;

        equityData.push(copy);
        break;
      case 'Simulation done':
        clearInterval(chartUpdating);
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
  if (debugEnabled) {
    console.log(msg);
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
  }
  $("#" + inputId).val(getQueryParam(qParam));
}
