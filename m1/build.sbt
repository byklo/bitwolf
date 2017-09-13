name := "m1"

version := "0.1"

scalaVersion := "2.12.1"

cancelable in Global := true

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-actor" % "2.5.4",
  "com.typesafe.akka" %% "akka-stream" % "2.5.4",
  "com.typesafe.play" %% "play-json" % "2.6.3"
)