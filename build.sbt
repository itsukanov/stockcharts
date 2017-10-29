import Dependencies._

organization := "com.itsukanov"

scalaVersion in ThisBuild := "2.12.3"

lazy val root = project.in(file("."))
  .aggregate(extractor, simulation, ui, kafka)

lazy val common = project.in(file("common"))
  .settings(commonDeps)

lazy val extractor = project.in(file("extractor"))
  .dependsOn(common)
  .settings(extractorDeps)

lazy val simulation = project.in(file("simulation"))
  .dependsOn(common)
  .settings(simulationDeps)

lazy val ui = project.in(file("ui"))
  .dependsOn(common)
  .settings(uiDeps)

lazy val kafka = project.in(file("kafka"))
  .dependsOn(common)
  .settings(kafkaDeps)
