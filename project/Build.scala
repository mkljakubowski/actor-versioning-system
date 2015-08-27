import sbt._
import sbt.Keys._

object Build extends Build {

	val akkaVersion = "2.3.12"
  val sprayVersion = "1.3.3"

	val tests = Seq(
		"org.scalatest" %% "scalatest" % "2.2.4" % "test",
		"junit" % "junit" % "4.11" % "test",
		"com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
	)

	val akka = Seq(
		"com.typesafe.akka" %% "akka-actor" % akkaVersion,
		"com.typesafe.akka" %% "akka-testkit" % akkaVersion
	)

  val spray = Seq(
		"io.spray" %% "spray-can" % sprayVersion
	)

  val bcel = Seq(
    "org.apache.bcel" % "bcel" % "5.2"
  )

	val typesafe = Seq(
		Classpaths.typesafeReleases,
		Classpaths.typesafeSnapshots,
		"Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
		"Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
	)

	lazy val baseDeps = tests ++ akka ++ spray ++ bcel

	val appName = "akka-avs"
	val appVersion = "0.1-SNAPSHOT"

	// Build definition
	lazy val buildSettings = Seq(
		organization := "org.virtuslab",
		version := appVersion,
		scalaVersion := "2.10.5"
	)

	// resolvers, dependencies and so on
	lazy val defaultSettings = buildSettings ++ Seq(
    fork := false,
    resolvers ++= typesafe,
		libraryDependencies ++= baseDeps,
    publishMavenStyle := false,
	  scalacOptions ++= Seq("-feature", "-language:implicitConversions")
	)

	val alsoOnTests = "compile->compile;test->test"

  lazy val root = sbt.Project(
    id = appName,
    base = file("."),
    settings = Project.defaultSettings ++ defaultSettings ++ Seq(publish := ())
  ) aggregate (core, plugin)

  lazy val core = sbt.Project(
    id = appName + "-core",
    base = file("core"),
    settings = Project.defaultSettings ++ defaultSettings
  ).settings(
		scalaVersion := "2.10.5"
	)

  lazy val plugin = sbt.Project(
    id = appName + "-plugin",
    base = file("plugin"),
    settings = Project.defaultSettings ++ defaultSettings ++ Seq(sbtPlugin := true)
  ) dependsOn core

}
