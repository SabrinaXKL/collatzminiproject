ThisBuild / version := "0.2"

ThisBuild / scalaVersion := "3.3.7"

import sbtassembly.AssemblyPlugin.autoImport._

assemblyMergeStrategy in assembly := {
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case PathList("module-info.class") => MergeStrategy.discard
  case x => MergeStrategy.first
}

mainClass in assembly := Some("com.collatzminiproject.Main")

val http4sVersion = "1.0.0-M45"
val circeVersion = "0.15.0-M1"

lazy val root = (project in file("."))
  .settings(
    name := "collatzminiproject",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "org.typelevel" %% "log4cats-core" % "2.7.1",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.1",
      "org.apache.logging.log4j" % "log4j-slf4j2-impl" % "2.24.0",
      "com.typesafe" % "config" % "1.4.3",
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test,
      "org.http4s" %% "http4s-ember-client" % http4sVersion  % Test,
      "org.scalatestplus" %% "scalacheck-1-18" % "3.2.19.0" % Test,
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
      "org.typelevel" %% "scalacheck-effect-munit" % "1.0.4" % Test


    )
  )
