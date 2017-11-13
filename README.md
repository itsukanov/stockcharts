## Motivation

Why did I write this project?

The first reason is that I wanted to try several new technologies such as akka-stream, docker and Kubernetes.
Hello world examples on the internet are good for getting started but usually, it's not enough to be sure that you can use a technology indeed.
It'll be much better to write a nontrivial and working application using these new technologies.

The second reason is that during job interviews I was sometimes asked to show examples of code written by me.
Stockcharts can be used for this purpose, also, I hope it helps me to save time by skipping writing a test task.

## Stockcharts

You can try working application [here](http://35.193.177.69:8080/simulate?stock=IBM&overbought=70&oversold=30&takeProfit=20&stopLoss=4).
The app is running in Google Cloud Platform over Kubernetes.
I used free tier of GCP without static ips or DNS names so there can be some issues with availability through the link above (e.g. the app was accidentally redeployed to a new ip).


A few words about the application, an example page from Stockcharts:

![Stockcharts example page](https://github.com/iatsukanov/stockcharts/blob/feature/readme/docs/stockcharts_example_page.png)

As you can see it's a single page application where you can test simple trading strategies on historical data.
You can set conditions for opening orders by choosing overbought and oversold levels of RSI indicator and conditions for closing orders by setting take profit and stop loss values.
The result of testing is the chart where you can find your balance and equity changes and events about opening and closing orders.


## Architecture

![Architecture](https://github.com/iatsukanov/stockcharts/blob/feature/readme/docs/stockcharts_architecture.png)

#### UI
A user through a Load Balancer (LB) goes to one of the UI instances.
When the user filled inputs and pressed 'Start simulation' button UI through a LB sends a request to one of the backend nodes to initialize a websocket connection.

UI using the websocket sends user's input data as a config to the server and waits for values for the chart such as prices, indicator values, balance, equity and trade events.

#### Simulation

When a simulation backend receives the trading strategy config it builds a stream:
* reads prices from kafka
* from prices builds indicator values
* from indicator values builds trade signals (should open buy or sell position)
* from trade signals builds trade events: opened/closed a buy/sell order; balance and equity values

All required data (prices, balance and so on) via json serialization are transformed to ui models and sent back to UI.

#### Extractor

Extractor should check current prices state in kafka and if it's necessary load prices from external sources into the kafka.

#### Scaling out and fault tolerance

Every part of the system is working in a Kubernetes cluster.

Kafka as a prices storage, of course, can be easily scaled out and continue working in the case of partial losing nodes.
In my case, I used a single node [embedded kafka](https://github.com/manub/scalatest-embedded-kafka) just for simplifying the development.

UI and simulation backends have more instances than it's necessary to handle the load in order to keep the system alive in case of losing nodes, problems with containers, etc.
For scaling out we have to do one easy step - say to Kubernetes that we need a bit more replicas.

A single instance of Extractor is normal because Stockcharts uses day candles and it's not a problem to wait a few seconds until Kubernetes redeploy the extractor container in case of any troubles.


## Implementation details

As mentioned before all parts of the system are built into docker containers and run in a Kubernetes cluster.
I get prices from [quandl](https://www.quandl.com). Base technologies are akka, akka-http and akka-stream.
The most interesting part of Stockcharts is the simulation of a trading strategy.
If you want to take a look at the implementation you can start with the [simulation route](https://github.com/iatsukanov/stockcharts/blob/master/simulation/src/main/scala/stockcharts/simulation/Routing.scala)
or from [unit tests](https://github.com/iatsukanov/stockcharts/tree/feature/readme/simulation/src/test/scala/stockcharts/simulation).
Briefly, there are several flows that enrich data.
For keeping state (to calculate new indicator value we have to know several previous prices, to calculate new balance we have to know previous balance and so on) they use actors.


## Start up

If you want to run Stockcharts on your local machine you can use:

### Sbt

Just run every kind of applications using commands:

```bash
sbt kafka/run
sbt extractor/run
sbt simulation/run
sbt ui/run
```

Default configuration you can find in `./stockcharts/common/src/main/resources/reference.conf`.
Keep in mind that the extractor uses 10 second pauses between extracting prices from quandl for every stock in order not to get `Too many requests` error.
This is a consequence of using a free quandl account.
So, prices will not be available immediately after the applications were started up.
The extractor needs about a minute to finish loading.

### Kubernetes (a real cluster)

1. build docker images:

```bash
sbt kafka/docker:publishLocal
sbt extractor/docker:publishLocal
sbt simulation/docker:publishLocal
sbt ui/docker:publishLocal
```

2. make them available for Kubernetes - push the images into your remote docker-registry

3. create services in Kubernetes:

```bash
kubectl create -f kubernetes/kafka.yaml
kubectl create -f kubernetes/extractor.yaml
kubectl create -f kubernetes/simulation.yaml
```

4. before deploying ui set up `SIMULATION_HOST` and `SIMULATION_PORT` in `./kubernetes/ui.yaml`.
Execute `kubectl get services | grep simulation`.
Use first value (8081 by default) from the `PORT(S)` column for `SIMULATION_PORT` and `EXTERNAL-IP` for `SIMULATION_HOST`.

5. deploy ui-service:

```bash
kubectl create -f kubernetes/ui.yaml
```

### Minikube

1. make local docker images available for Minikube - execute `eval $(minikube docker-env)`

2. build docker images:

```bash
sbt kafka/docker:publishLocal
sbt extractor/docker:publishLocal
sbt simulation/docker:publishLocal
sbt ui/docker:publishLocal
```

3. create services in Kubernetes:

```bash
kubectl create -f kubernetes/kafka.yaml
kubectl create -f kubernetes/extractor.yaml
kubectl create -f kubernetes/simulation.yaml
```

4. before deploying ui set up `SIMULATION_HOST` and `SIMULATION_PORT` in `./kubernetes/ui.yaml`.
Execute `minikube ip` and use the result for `SIMULATION_HOST`.
Execute `kubectl get services | grep simulation` and use second value from the `PORT(S)` column for `SIMULATION_PORT`.

5. deploy ui-service:

```bash
kubectl create -f kubernetes/ui.yaml
```

## Other comments

UI doesn't have features such as you can't set the lot size for orders, initial balance and so on.
I didn't spend time for their realization because it was outside of my aims (see the [motivation](#motivation) part).
Also, there are several bugs:
* pins for opening and closing orders hide each other when they are on the same candle
* the animation speed degrades with increasing amount of data on the chart
* maybe something else

Quality of the ui code and design of the page are far from ideal because I'm a backend developer.

Some parts of scala code might look complicated.
When you work alone on the project without code reviews and when you wrote the entire code base it sometimes seems that everything is obvious.

Stockcharts doesn't have price data tick-by-tick it works with candles, 4 prices (open, high, low, close) inside a day.
If we have an order and high tells us to take profit whereas low tells us to stop loss what should we do?
They have the same date - the date of the candle and there is no chance to determine what was earlier.
I chose to work only with closing prices.
Of course, it's wrong from the user's point of view but such behavior was enough for my aims.
Also, the price can be changed significantly over a day, an order can be closed with too much loss and your balance will become a little bit negative.