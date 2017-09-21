import sbt._
import sbt.Keys._

object Dependencies {

  lazy val commonDeps = libraryDependencies ++= Seq(

  )

  lazy val externalSourceExtractorDeps = libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % "10.0.9",
    "org.json4s" %% "json4s-native" % "3.5.3"
  )

  lazy val uiDeps = libraryDependencies ++= Seq(

  )

}
