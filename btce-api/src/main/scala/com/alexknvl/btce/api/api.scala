package com.alexknvl.btce.api

import scala.util.control.NonFatal

case class ApiException(
  message: String = null,
  cause: Throwable = null
) extends RuntimeException(message, cause)

class PublicApi {
  private final val ApiUrl = "https://btc-e.com/api/3/"
  import spray.json._
  import Protocol._

  def request(apiString: String): Either[Error, JsObject] = {
    import org.apache.http.client.fluent.Request

    try {
      val uri = ApiUrl + apiString
      val response = Request.Get(uri).execute()
      val json = JsonParser(response.returnContent().asString()).asJsObject

      json match {
        case Error(error: Error) => Left(error)
        case _ => Right(json)
      }
    } catch {
      case ex: ApiException => throw ex
      case NonFatal(ex) => throw ApiException(ex.getMessage, ex)
    }
  }
  def request(method: String, pairs: Traversable[Pair], ignoreInvalid: Boolean = false): Either[Error, JsObject] =
    request(s"$method/${pairs.mkString("-")}${if (ignoreInvalid) "?ignore_invalid=1" else ""}")

  def ticker(pair: Pair): Either[Error, Ticker] =
    request("ticker", Traversable(pair), false).right.map {
      _.fields(pair.toString).convertTo[Ticker]
    }
  def ticker(pairs: Traversable[Pair], ignoreInvalid: Boolean = false): Either[Error, Map[Pair, Ticker]] =
    request("ticker", pairs, ignoreInvalid).right.map {
      _.fields map { case (k, v) => (Pair(k), v.convertTo[Ticker]) }
    }

  def trades(pair: Pair): Either[Error, List[Trade]] =
    request("trades", Traversable(pair), false).right.map {
      _.fields(pair.toString).convertTo[List[Trade]]
    }
  def trades(pairs: Traversable[Pair], ignoreInvalid: Boolean = false): Either[Error, Map[Pair, List[Trade]]] =
    request("trades", pairs, ignoreInvalid).right.map {
      _.fields map { case (k, v) => (Pair(k), v.convertTo[List[Trade]]) }
    }

  def depth(pair: Pair): Either[Error, Depth] =
    request("depth", Traversable(pair), false).right.map {
      _.fields(pair.toString).convertTo[Depth]
    }
  def depth(pairs: Traversable[Pair], ignoreInvalid: Boolean = false): Either[Error, Map[Pair, Depth]] =
    request("depth", pairs, ignoreInvalid).right.map {
      _.fields map { case (k, v) => (Pair(k), v.convertTo[Depth]) }
    }
}

class TradeApi(private val key: String, private val secret: String) {
  import spray.json._
  import Protocol._

  private final val ApiUrl = "https://btc-e.com/tapi/"

  val auth = new Auth(key, secret)

  private def toPostData(args: Map[String, Any]) =
    args map { case (a, b) => a + "=" + b.toString} mkString "&"

  private def rawRequest(method: String, args: Map[String, String] = Map()): String = {
    import org.apache.http.client.fluent.Request
    import org.apache.http.entity.ContentType
    import org.apache.http.message.BasicHeader

    try {
      val data = toPostData(args + ("method" -> method, "nonce" -> auth.newNonce))

      val request = Request.Post(ApiUrl)
      request.addHeader(new BasicHeader("Key", auth.key))
      request.addHeader(new BasicHeader("Sign", auth.sign(data)))
      request.bodyString(data, ContentType.APPLICATION_FORM_URLENCODED)

      val response = request.execute()
      response.returnContent().asString()
    } catch {
      case NonFatal(ex) => throw ApiException(ex.getMessage, ex)
    }
  }

  private def parse(text: String): Either[Error, spray.json.JsObject] = {
    try {
      JsonParser(text) match {
        case Error(err) => Left(err)
        case obj: JsObject => Right(obj.getFields("return")(0).asJsObject)
        case _ => throw ApiException("Invalid response format.")
      }
    } catch {
      case NonFatal(ex) => throw ApiException(ex.getMessage, ex)
    }
  }

  def request(method: String, args: Map[String, String] = Map()): Either[Error, spray.json.JsObject] = {
    parse(rawRequest(method, args)) match {
      case Left(InvalidNonce(current: Long, sent: Long)) =>
        auth.nonce = current + 1
        this.request(method, args)
      case value => value
    }
  }

  def accountInfo: Either[Error, AccountInfo] =
    request("getInfo").right.map { _.convertTo[AccountInfo] }

  def transactionHistory: Either[Error, Map[BigInt, TransactionHistoryEntry]] =
    transactionHistory(None, None, None, None, None, None, None)

  def transactionHistory(from: Option[Long] = None, count: Option[Long] = None,
                         fromId: Option[Long] = None, endId: Option[Long] = None,
                         order: Option[String] = None, since: Option[Long] = None,
                         end: Option[Long] = None): Either[Error, Map[BigInt, TransactionHistoryEntry]] = {
    val arguments: Map[String, String] = Map(
      "from" -> from, "count" -> count, "from_id" -> fromId, "end_id" -> endId, "order" -> order,
      "since" -> since, "end" -> end)
      .filter { case (k, v) => !v.isEmpty }
      .map { case(k, v) => (k, v.get.toString)}

    request("TransHistory", arguments).right.map { _.convertTo[Map[BigInt, TransactionHistoryEntry]] }
  }

  def tradeHistory: Either[Error, Map[BigInt, TradeHistoryEntry]] =
    tradeHistory(None, None, None, None, None, None, None)

  def tradeHistory(from: Option[Long] = None, count: Option[Long] = None,
                   fromId: Option[Long] = None, endId: Option[Long] = None,
                   order: Option[String] = None, since: Option[Long] = None,
                   end: Option[Long] = None): Either[Error, Map[BigInt, TradeHistoryEntry]] = {
    val arguments: Map[String, String] = Map(
      "from" -> from, "count" -> count, "from_id" -> fromId, "end_id" -> endId, "order" -> order,
      "since" -> since, "end" -> end)
      .filter { case (k, v) => !v.isEmpty }
      .map { case(k, v) => (k, v.get.toString)}

    request("TradeHistory", arguments).right.map { _.convertTo[Map[BigInt, TradeHistoryEntry]] }
  }

  def activeOrders: Either[Error, Map[BigInt, OrderListEntry]] = activeOrders(None)

  def activeOrders(pair: Option[Pair] = None): Either[Error, Map[BigInt, OrderListEntry]] = {
    val arguments: Map[String, String] = Map("pair" -> pair)
      .filter { case (k, v) => !v.isEmpty }
      .map { case(k, v) => (k, v.get.toString()) }

    request("ActiveOrders", arguments) match {
      case Right(v) => Right(v.convertTo[Map[BigInt, OrderListEntry]])
      case Left(NoOrders()) => Right(Map[BigInt, OrderListEntry]())
      case Left(err) => Left(err)
    }
  }

  def trade(pair: Pair, tpe: String, rate: BigDecimal, amount: BigDecimal) = {
    val arguments: Map[String, String] = Map(
      "pair" -> pair.toString,
      "type" -> tpe.toString,
      "rate" -> rate.toString,
      "amount" -> amount.toString
    )

    request("Trade", arguments).right.map { _.convertTo[TradeResponse] }
  }

  def cancelOrder(orderId: BigInt) = {
    val arguments: Map[String, String] = Map(
      "order_id" -> orderId.toString()
    )

    request("CancelOrder", arguments).right.map { _.convertTo[CancelOrderResponse] }
  }
}
