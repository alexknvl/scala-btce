package com.alexknvl.btce.chat

import com.pusher.client.{PusherOptions, Pusher}
import com.pusher.client.connection.{ConnectionStateChange, ConnectionEventListener, ConnectionState}
import com.pusher.client.channel.SubscriptionEventListener
import reactive.EventSource

case class ChatMessage(userId: String, user: String, message: String, messageId: Long, date: String,
                       userColor: String)

private[chat] object Protocol extends spray.json.DefaultJsonProtocol {
  import spray.json._
  implicit val ChatMessageFormat = jsonFormat(ChatMessage, "uid", "login", "msg", "msg_id",
                                              "date", "usr_clr")
}


class ChatApi {
  private final val ApiKey = "4e0ebd7a8b66fa3554a4"
  private val pusherOptions = new PusherOptions
  pusherOptions.setEncrypted(true)
  private val pusher = new Pusher(ApiKey, pusherOptions)
  private val subscriptions = new scala.collection.mutable.HashSet[String]

  val messageSource = new EventSource[(String, ChatMessage)] { }

  def subscribe(locales: Iterable[String]): Unit = {
    import spray.json._
    import Protocol._

    for (locale <- locales) {
      if (!subscriptions.contains(locale)) {
        subscriptions += locale
        val channel = pusher.subscribe("chat_" + locale)

        channel.bind("msg", new SubscriptionEventListener {
          def onEvent(channelName: String, eventName: String, data: String): Unit = {
            val locale = channelName.substring("chat_".size)
            val JsString(jsonText) = JsonParser(data)
            val message = JsonParser(jsonText).convertTo[ChatMessage]
            messageSource.fire((locale, message))
          }
        })
      }
    }
  }
  def subscribe(locales: String*): Unit = subscribe(locales)

  def unsubscribe(locales: Iterable[String]): Unit = {
    for (locale <- locales) {
      if (subscriptions.contains(locale)) {
        pusher.unsubscribe("chat_" + locale)
        subscriptions -= locale
      }
    }
  }
  def unsubscribe(locales: String*): Unit = unsubscribe(locales)

  def connect() = pusher.connect()
  def disconnect() = pusher.disconnect()

}