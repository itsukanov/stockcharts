package stockcharts.kafka

import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object KafkaUtils {

  val log = LoggerFactory.getLogger(this.getClass)

  def isTopicEmpty(topic: String, timeOut: FiniteDuration)
                  (implicit system: ActorSystem, materializer: ActorMaterializer): Future[Boolean] = {
    import system.dispatcher

    KafkaSource(topic, groupId = s"checking_for_empty_$topic", OffsetReset.Earliest)
      .completionTimeout(timeOut)
      .toMat(Sink.headOption)(Keep.right)
      .run()
      .recover {
        case e: TimeoutException => None
        case thr =>
          log.error(s"Can't check $topic is empty or not", thr)
          throw thr
      }.map(_.isEmpty)
  }

}
