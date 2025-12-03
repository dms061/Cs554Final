name := "examples"

version := "0.1"

scalaVersion := "2.13.18"

val pekkoVersion = "1.3.0"
val logbackVersion = "1.3.15"

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion)

idePackagePrefix := Some("uic.cs554")

