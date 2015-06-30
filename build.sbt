name := "discosearch"

version := "1.0"

scalaVersion := "2.11.6"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "io.spray" %% "spray-can" % "1.3.2",
  "io.spray" %% "spray-json" % "1.3.2",
  "io.spray" %% "spray-routing" % "1.3.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "edu.arizona.sista" %% "processors" % "5.4-SNAPSHOT",
  "edu.arizona.sista" %% "processors" % "5.4-SNAPSHOT" classifier "models"
)
