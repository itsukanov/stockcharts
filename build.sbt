import Dependencies._

organization := "com.itsukanov"

scalaVersion in ThisBuild := "2.12.3"

val root = project.in(file("."))

val common = project.in(file("common"))
  .settings(commonDeps)

val `external-source-extractor` = project.in(file("external-source-extractor"))
  .dependsOn(common)
  .settings(externalSourceExtractorDeps)

val simulation = project.in(file("simulation"))
  .dependsOn(common)
  .settings(simulationDeps)

val ui = project.in(file("ui"))
  .dependsOn(common)
  .settings(uiDeps)

val `embedded-kafka` = project.in(file("embedded-kafka"))
  .dependsOn(common)
  .settings(kafkaDeps)
