name := "btce-api"

scalaVersion := "2.10.3"

resolvers += "spray" at "http://repo.spray.io/"

libraryDependencies ++= Seq(
    "org.apache.httpcomponents" % "httpclient" % "4.3.1",
    "org.apache.httpcomponents" % "fluent-hc" % "4.3.1",
    "io.spray" %%  "spray-json" % "1.2.5"
)
