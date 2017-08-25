import Dependencies._

organization := "itsukanov.com"

scalaVersion := "2.12.3"

lazy val root = project.in(file("."))

lazy val common = project.in(file("common"))
  .settings(commonDeps)

lazy val `writer-in-kafka` = project.in(file("writer-in-kafka"))
  .dependsOn(common)
  .settings(writerInKafkaDeps)
//  .settings(mainClass in assembly := Some("casino.fraud.inspector.Inspector"))
//  .settings(
//    name := "inspector",
//    assemblyMergeStrategy in assembly <<= (assemblyMergeStrategy in assembly) { old =>
//      {
//        case PathList("org", "apache", xs@_*) => MergeStrategy.last
//        case PathList("com", "esotericsoftware", xs@_*) => MergeStrategy.last
//        case PathList("com", "google", "common", "base", xs@_*) => MergeStrategy.last
//        case PathList("javax", "xml", xs@_*) => MergeStrategy.last
//        case x => old(x)
//      }
//    }
//  )

