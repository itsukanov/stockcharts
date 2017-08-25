import Dependencies._

organization := "com.itsukanov"

scalaVersion in ThisBuild := "2.12.3"

lazy val root = project.in(file("."))

lazy val common = project.in(file("common"))
  .settings(commonDeps)

lazy val `external-source-extractor` = project.in(file("external-source-extractor"))
  .dependsOn(common)
  .settings(externalSourceExtractorDeps)
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
