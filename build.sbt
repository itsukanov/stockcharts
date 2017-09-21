import Dependencies._

organization := "com.itsukanov"

scalaVersion in ThisBuild := "2.12.3"

lazy val root = project.in(file("."))

lazy val common = project.in(file("common"))
  .settings(commonDeps)

lazy val `external-source-extractor` = project.in(file("external-source-extractor"))
  .dependsOn(common)
  .settings(externalSourceExtractorDeps)

lazy val ui = project.in(file("ui"))
  .dependsOn(common)
  .settings(uiDeps)
