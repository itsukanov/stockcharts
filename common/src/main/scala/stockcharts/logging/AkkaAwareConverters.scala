package stockcharts.logging

import ch.qos.logback.classic.pattern.{ClassicConverter, LoggerConverter}
import ch.qos.logback.classic.spi.ILoggingEvent

import scala.collection.JavaConverters._


/**
  * Logback thread name converter aware of [[akka.event.slf4j.Slf4jLogger]]
  * using MDC for storing akka source thread.
  */
class AkkaAwareThreadConverter extends ClassicConverter {
  override def convert(event: ILoggingEvent): String = {
    val mdc = event.getMDCPropertyMap.asScala
    mdc.getOrElse("sourceThread", event.getThreadName)
  }
}

/**
  * Logback logger converter aware of [[akka.event.slf4j.Slf4jLogger]]
  * using MDC for storing akka source.
  */
class AkkaAwareLoggerConverter extends LoggerConverter {
  override def getFullyQualifiedName(event: ILoggingEvent): String = {
    val loggerName = super.getFullyQualifiedName(event)

    val mdc = event.getMDCPropertyMap.asScala

    mdc.get("akkaSource") match {
      case Some(akkaSource) => s"$loggerName[$akkaSource]"
      case None => loggerName
    }
  }
}
