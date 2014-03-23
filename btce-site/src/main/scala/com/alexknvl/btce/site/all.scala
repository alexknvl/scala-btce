package com.alexknvl.btce.site

import org.jsoup.Jsoup
import org.jsoup.Connection
import org.jsoup.nodes.Document

case class ParseException(message: String = null, cause: Throwable = null) extends RuntimeException(message, cause)

private class Auth {
  private final val BodyCookieScriptPattern =
    "document.cookie=\"a=([a-f0-9]{32});path=/;\";location.href=document.URL;".r

  private var bodyCookie: Option[String] = None

  def updateCookies(response: Connection.Response, doc: Document): Boolean = {
    val title = doc.select("head title").first()

    if (title.text() == "loading") {
      doc.select("body script").first().data() match {
        case BodyCookieScriptPattern(bodyCookie) => this.bodyCookie = Some(bodyCookie)
        case _ => throw ParseException("Could not parse body cookie script.")
      }

      true
    } else false
  }

  def setCookies(connection: Connection) = bodyCookie.foreach { connection.cookie("a", _) }
}

case class ChatMessage(
  id: Long,
  time: String,
  user: String,
  message: String)

case class ScrapingResult(
  messages: List[ChatMessage],
  userCount: Long,
  botCount: Long,
  isDevOnline: Boolean,
  isSupportOnline: Boolean,
  isAdminOnline: Boolean)

class SiteApi {
  private final val SiteUrl = "https://btc-e.com/"
  private final val UsersEnPattern = "Users: (\\d+) Bots: (\\d+)".r
  private final val UsersRuPattern = "Пользователи: (\\d+) Боты: (\\d+)".r
  private val auth = new Auth

  private def mainPage(locale: String = "en"): Document = mainPage(locale, 0)
  private def mainPage(locale: String, tries: Int): Document = {
    val connection = Jsoup.connect(SiteUrl)
    auth.setCookies(connection)
    connection.cookie("locale", locale)
    val response = connection.execute()
    val doc = response.parse()

    if (auth.updateCookies(response, doc)) {
      if (tries < 3) mainPage(locale, tries + 1)
      else throw ParseException("Could not update cookies.")
    } else doc
  }

  private def scrape(doc: Document): ScrapingResult = {
    import scala.collection.JavaConversions._

    val (userCount, botCount) = doc.select("div#users-online").first().ownText() match {
      case UsersEnPattern(users, bots) => (users.toLong, bots.toLong)
      case UsersRuPattern(users, bots) => (users.toLong, bots.toLong)
      case _ => throw ParseException("Could not parse user/bot count.")
    }

    var isDevOnline = false
    var isSupportOnline = false
    var isAdminOnline = false

    for (elem <- doc.select("div#users-online p a[href]").iterator()) {
      elem.attr("href") match {
        case "https://btc-e.com/profile/1" => isDevOnline = true
        case "https://btc-e.com/profile/2" => isSupportOnline = true
        case "https://btc-e.com/profile/3" => isAdminOnline = true
        case _ => ()
      }
    }

    val messages = for (
      elem <- doc.select("div#nChat p.chatmessage").listIterator();
      id = elem.id().substring(3).toLong;
      a = elem.select("a").get(0);
      time = a.attr("title");
      user = a.text();
      message = elem.select("span").get(0).text()
    ) yield ChatMessage(id, time, user, message)

    ScrapingResult(messages.toList, userCount, botCount, isDevOnline, isSupportOnline, isAdminOnline)
  }

  def scrape(locale: String = "en"): ScrapingResult = scrape(mainPage(locale))
  def scrape: ScrapingResult = scrape()
}