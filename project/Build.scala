import bintray.BintrayKeys._
import com.typesafe.sbt.SbtGit.GitKeys._
import sbt.Keys._
import sbt._

object Build extends sbt.Build {
  lazy val root = Project("kamon-influxdb", base = file("."))
    .settings(
      name := "kamon-influxdb",
      organization := "de.choffmeister",
      version := gitDescribedVersion.value.map(_.drop(1)).get,
      scalaVersion := "2.11.8",
      scalacOptions ++= Seq("-encoding", "utf8", "-deprecation", "-feature"),
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.4.3",
        "com.typesafe.akka" %% "akka-testkit" % "2.4.3" % "test",
        "io.kamon" %% "kamon-core" % "0.5.2",
        "org.scalatest" %% "scalatest" % "2.2.6" % "test"
      )
    )
    .settings(publishSettings: _*)

  lazy val publishSettings = Seq(
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    bintrayReleaseOnPublish in ThisBuild := false
  )
}
