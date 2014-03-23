package com.alexknvl.btce.api

import spray.json._

case class Currency(name: String) {
  override def toString = name
}

case class Pair(first: Currency, second: Currency) {
  override def toString = {
    val Currency(firstName) = first
    val Currency(secondName) = second

    firstName + "_" + secondName
  }
}
object Pair {
  def apply(firstName: String, secondName: String): Pair =
    Pair(Currency(firstName), Currency(secondName))
  def apply(pairName: String): Pair = {
    pairName.split('_') match {
      case Array(a, b) => Pair(a, b)
      case _ => throw new IllegalArgumentException(
        "Pair name should consist of currency names separated by '_'.")
    }
  }
}

case class Funds(
  usd: BigDecimal,
  btc: BigDecimal,
  ltc: BigDecimal,
  nmc: BigDecimal,
  rur: BigDecimal,
  eur: BigDecimal,
  nvc: BigDecimal,
  trc: BigDecimal,
  ppc: BigDecimal,
  ftc: BigDecimal,
  xpm: BigDecimal)

private[btce] trait CommonApiFormats extends DefaultJsonProtocol {
  implicit object CurrencyFormat extends RootJsonFormat[Currency] {
    def write(currency: Currency) = JsString(currency.name)

    def read(value: JsValue) =
      value match {
        case JsString(name) => Currency(name.toLowerCase)
        case _ => throw new DeserializationException("Expected currency name.")
      }
  }
  implicit object PairFormat extends RootJsonFormat[Pair] {
    def write(pair: Pair) = JsString(pair.toString())

    def read(value: JsValue) =
      value match {
        case JsString(name) => Pair(name.toLowerCase)
        case _ => throw new DeserializationException("Expected pair of currencies.")
      }
  }
  implicit val FundsFormat = jsonFormat11(Funds)
}

case class Info(
  serverTime: Long,
  pairs: Map[Pair, PairInfo])
case class PairInfo(
  decimalPlaces: Int,
  minPrice: BigDecimal,
  maxPrice: BigDecimal,
  minAmount: BigDecimal,
  hidden: Boolean,
  fee: BigDecimal)
case class Rights(
  info: Boolean,
  trade: Boolean,
  withdraw: Boolean)
case class AccountInfo(
  funds: Funds,
  rights: Rights,
  transactionCount: Long,
  openOrders: Long,
  serverTime: Long)
case class Ticker(
  high: BigDecimal,
  low: BigDecimal,
  avg: BigDecimal,
  vol: BigDecimal,
  vol_cur: BigDecimal,
  last: BigDecimal,
  buy: BigDecimal,
  sell: BigDecimal,
  updated: Long)
case class Trade(
  tpe: String,
  price: BigDecimal,
  amount: BigDecimal,
  tid: BigInt,
  timestamp: Long)
case class Depth(
  asks: List[(BigDecimal, BigDecimal)],
  bids: List[(BigDecimal, BigDecimal)])

private[btce] trait PublicApiFormats extends CommonApiFormats {
  implicit object PairInfoFormat extends RootJsonFormat[PairInfo] {
    def write(pairInfo: PairInfo) = JsObject(
      "decimal_places" -> JsNumber(pairInfo.decimalPlaces),
      "min_price" -> JsNumber(pairInfo.minPrice),
      "max_price" -> JsNumber(pairInfo.maxPrice),
      "min_amount" -> JsNumber(pairInfo.minAmount),
      "hidden" -> JsNumber(if (pairInfo.hidden) 1 else 0),
      "fee" -> JsNumber(pairInfo.fee))

    def read(value: JsValue): PairInfo =
      value.asJsObject.getFields(
        "decimal_places", "min_price", "max_price", "min_amount", "hidden", "fee"
      ) match {
        case Seq(JsNumber(decimalPlaces), JsNumber(minPrice), JsNumber(maxPrice), JsNumber(minAmount),
        JsNumber(hidden), JsNumber(fee)) =>
          PairInfo(decimalPlaces.toInt, minPrice, maxPrice, minAmount, hidden != BigDecimal(0), fee)
        case _ => throw new DeserializationException("Expected pair of currencies.")
      }
  }
  implicit val InfoFormat = jsonFormat(Info,
    "server_time", "pairs")

  implicit object RightsFormat extends RootJsonFormat[Rights] {
    def write(rights: Rights) = JsObject(
      "info" -> JsNumber(if (rights.info) 1 else 0),
      "trade" -> JsNumber(if (rights.trade) 1 else 0))

    def read(value: JsValue) =
      value.asJsObject.getFields("info", "trade", "withdraw") match {
        case Seq(JsNumber(info), JsNumber(trade), JsNumber(withdraw)) =>
          Rights(info != BigDecimal(0), trade != BigDecimal(0), withdraw != BigDecimal(0))
        case _ => throw new DeserializationException("Expected Rights object.")
      }
  }

  implicit val AccountInfoFormat = jsonFormat(AccountInfo,
    "funds", "rights", "transaction_count", "open_orders", "server_time")
  implicit val TickerFormat = jsonFormat9(Ticker)
  implicit val TradeFormat = jsonFormat(Trade,
    "type", "price", "amount", "tid", "timestamp")
  implicit val DepthFormat = jsonFormat2(Depth)
}

case class TransactionHistoryEntry(
  tpe: Int,
  amount: BigDecimal,
  currency: Currency,
  desc: String,
  status: Int,
  timestamp: Long)
case class TradeHistoryEntry(
  orderId: BigInt,
  pair: Pair,
  tpe: String,
  amount: BigDecimal,
  rate: BigDecimal,
  isMine: Boolean,
  timestamp: Long)
case class OrderListEntry(
  pair: Pair,
  tpe: String,
  amount: BigDecimal,
  rate: BigDecimal,
  createdTimestamp: Long,
  status: Int)
case class TradeResponse(
  received: BigDecimal,
  remains: BigDecimal,
  orderId: Long,
  funds: Funds)
case class CancelOrderResponse(
  orderId: Long,
  funds: Funds)

private[btce] trait TradeApiFormats extends CommonApiFormats {
  implicit val TransactionHistoryEntryFormat = jsonFormat(TransactionHistoryEntry,
    "type", "amount", "currency", "desc", "status", "timestamp")

  implicit object TradeHistoryEntryFormat extends RootJsonFormat[TradeHistoryEntry] {
    def write(order: TradeHistoryEntry) = JsObject(
      "order_id" -> JsNumber(order.orderId),
      "pair" -> order.pair.toJson,
      "type" -> JsString(order.tpe),
      "amount" -> JsNumber(order.amount),
      "rate" -> JsNumber(order.rate),
      "is_your_order" -> JsNumber(if (order.isMine) 1 else 0),
      "timestamp" -> JsNumber(order.timestamp))

    def read(value: JsValue) =
      value.asJsObject.getFields(
        "order_id",
        "pair",
        "type",
        "amount",
        "rate",
        "is_your_order",
        "timestamp"
      ) match {
        case Seq(JsNumber(orderId), JsString(pair), JsString(tpe), JsNumber(amount),
        JsNumber(rate), JsNumber(isMine), JsNumber(timestamp)) =>
          TradeHistoryEntry(orderId.toBigInt(), Pair(pair), tpe, amount, rate, isMine != BigDecimal(0),
            timestamp.toLong)
        case _ => throw new DeserializationException("Expected TradeHistoryEntry object.")
      }
  }
  implicit val OrderListEntryFormat = jsonFormat(OrderListEntry,
    "pair", "type", "amount", "rate", "timestamp_created", "status")
  implicit val TradeResponseFormat = jsonFormat(TradeResponse,
    "received", "remains", "order_id", "funds")
  implicit val CancelOrderResponseFormat = jsonFormat(CancelOrderResponse,
    "order_id", "funds")
}

private[btce] object ApiFormats extends PublicApiFormats with TradeApiFormats