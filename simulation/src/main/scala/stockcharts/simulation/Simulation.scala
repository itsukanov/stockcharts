package stockcharts.simulation

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Source}
import stockcharts.models.SimulationConf

object SimulationFactory {

  def apply(conf: SimulationConf) = Flow[Message]
    .mapConcat {
      case tm: TextMessage =>
        TextMessage(Source.single("Hello ") ++ tm.textStream) :: Nil
    }

}
