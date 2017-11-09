import Dependencies._

version in ThisBuild := "0.1.0"
organization := "com.itsukanov"

scalaVersion in ThisBuild := "2.12.3"

val jre = "openjdk:jre-alpine"
val dockerRepo = Some("stockcharts")

lazy val root = project.in(file("."))
  .aggregate(extractor, simulation, ui, kafka)

lazy val common = project.in(file("common"))
  .settings(commonDeps)

lazy val extractor = project.in(file("extractor"))
  .dependsOn(common)
  .settings(extractorDeps)
  .enablePlugins(JavaAppPackaging, DockerPlugin, AshScriptPlugin)
  .settings(
    mainClass in Compile := Some("stockcharts.extractor.ExtractorApp"),
    dockerBaseImage      := jre,
    dockerRepository     := dockerRepo
  )

lazy val simulation = project.in(file("simulation"))
  .dependsOn(common)
  .settings(simulationDeps)
  .enablePlugins(JavaAppPackaging, DockerPlugin, AshScriptPlugin)
  .settings(
    mainClass in Compile := Some("stockcharts.simulation.SimulationApp"),
    dockerBaseImage      := jre,
    dockerRepository     := dockerRepo
  )

lazy val ui = project.in(file("ui"))
  .dependsOn(common)
  .settings(uiDeps)
  .enablePlugins(JavaAppPackaging, DockerPlugin, AshScriptPlugin)
  .settings(
    mainClass in Compile := Some("stockcharts.ui.UIApp"),
    dockerBaseImage      := jre,
    dockerRepository     := dockerRepo
  )

lazy val kafka = project.in(file("kafka"))
  .dependsOn(common)
  .settings(kafkaDeps)
