import Dependencies._

organization := "com.itsukanov"

scalaVersion in ThisBuild := "2.12.3"

val root = project.in(file("."))

val common = project.in(file("common"))
  .settings(commonDeps)

val extractor = project.in(file("extractor"))
  .dependsOn(common)
  .settings(extractorDeps)

val simulation = project.in(file("simulation"))
  .dependsOn(common)
  .settings(simulationDeps)

val ui = project.in(file("ui"))
  .dependsOn(common)
  .settings(uiDeps)

val kafka = project.in(file("kafka"))
  .dependsOn(common)
  .settings(kafkaDeps)
