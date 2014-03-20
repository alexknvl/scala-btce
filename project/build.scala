import sbt._
import sbt.Keys._

object ScalaBtceBuild extends Build {
    lazy val api = project.in(file("btce-api"))
    lazy val chat = project.in(file("btce-chat"))
    lazy val site = project.in(file("btce-site"))

    lazy val root = project.in(file(".")).aggregate(api, chat, site)
}
