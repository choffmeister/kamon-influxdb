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
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    pomExtra := mavenInfos)

  lazy val mavenInfos = {
    <url>https://github.com/choffmeister/kamon-influxdb</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>http://opensource.org/licenses/MIT</url>
      </license>
    </licenses>
    <scm>
      <url>github.com/choffmeister/kamon-influxdb.git</url>
      <connection>scm:git:github.com/choffmeister/kamon-influxdb.git</connection>
      <developerConnection>scm:git:git@github.com:choffmeister/kamon-influxdb.git</developerConnection>
    </scm>
    <developers>
      <developer>
        <id>choffmeister</id>
        <name>Christian Hoffmeister</name>
        <url>http://choffmeister.de/</url>
      </developer>
    </developers> }
}
