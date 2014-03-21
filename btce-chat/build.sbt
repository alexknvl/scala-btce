name := "btce-chat"

scalaVersion := "2.10.3"

organization := "com.alexknvl"

version := "0.1-SNAPSHOT"

resolvers += "spray" at "http://repo.spray.io/"

libraryDependencies ++= Seq(
  "com.pusher" % "pusher-java-client" % "0.3.1",
  "io.spray" %%  "spray-json" % "1.2.5",
  "cc.co.scala-reactive" %% "reactive-core" % "0.3.2.1"
)
