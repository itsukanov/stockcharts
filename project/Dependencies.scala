import sbt._
import sbt.Keys._

object Dependencies {
  import GlobalExclusions._

  val commonDeps = libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )

  val externalSourceExtractorDeps = libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % "10.0.9",
    "org.json4s" %% "json4s-native" % "3.5.3"
  )

  val uiDeps = libraryDependencies ++= Seq()

  val kafkaDeps = libraryDependencies ++= Seq(
    "net.manub" %% "scalatest-embedded-kafka-streams" % "0.15.1"
  ).withoutLog4j

}

object GlobalExclusions {

  private val log4jDeps = Seq(
    ExclusionRule("log4j", "log4j"),
    ExclusionRule("org.slf4j", "slf4j-log4j12")
  )
  private val fromLog4jToLogbackDep = "org.slf4j" % "log4j-over-slf4j" % "1.7.12"

  implicit class ModuleIDGlobalExclusions(val deps: Seq[ModuleID]) {
    def without(exclusions: Seq[ExclusionRule]): Seq[ModuleID] = deps.map(_.excludeAll(exclusions: _*))
    def without(exclusions: ExclusionRule): Seq[ModuleID] = without(Seq(exclusions))

    lazy val withoutLog4j = (deps without log4jDeps) :+ fromLog4jToLogbackDep
  }

}
