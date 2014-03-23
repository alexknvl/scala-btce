package com.alexknvl.btce.api

abstract class Error
case class InvalidNonce(current: Long, sent: Long) extends Error {
  override def toString = s"Invalid nonce parameter; on key:($current), you sent:($sent)."
}
case class InvalidPairName(name: Pair) extends Error {
  override def toString = s"Invalid pair name: $name."
}
case class InvalidMethod() extends Error {
  override def toString = "Invalid method."
}
case class NoOrders() extends Error {
  override def toString = "No orders."
}
case class NotEnoughCurrency(currency: Currency) extends Error {
  override def toString = s"Not enough ${currency.name} in the account for sale."
}
case class PriceTooHigh(first: Currency, value: BigDecimal, second: Currency) extends Error {
  override def toString = s"Price per ${first.name} must be less than $value ${second.name}."
}
case class VolumeTooLow(currency: Currency, minValue: BigDecimal) extends Error {
  override def toString = s"Volume of ${currency.name} must be higher than $minValue."
}
case class Unknown(message: String) extends Error {
  override def toString = message
}

object ErrorParser {
  import spray.json.{JsValue, JsObject, JsNumber, JsString}

  private val InvalidNoncePattern = "invalid nonce parameter; on key:(\\d+), you sent:(\\d+)".r
  private val InvalidPairNamePattern = "Invalid pair name: (\\w+)".r
  private val InvalidMethodPattern = "Invalid method"
  private val NoOrdersPattern = "no orders"
  private val NotEnoughCurrencyPattern = "It is not enough (\\w+) in the account for sale.".r
  private val PriceTooHighPattern = "Price per (\\w+) must be less ([\\d\\.]+) (\\w+).".r
  private val VolumeTooLowPattern = "Value (\\w+) must be greater than ([\\d\\.]+) (\\w+).".r

  def unapply(json: JsValue): Option[Error] = json match {
    case obj: JsObject => this.unapply(obj)
    case _ => None
  }

  def unapply(json: JsObject): Option[Error] =
    json.getFields("success", "error") match {
      case Seq(JsNumber(code), JsString(message)) if code == BigDecimal(0) => this.unapply(message)
      case _ => None
    }

  def unapply(error: String): Option[Error] =
    error match {
      case InvalidNoncePattern(cur, sent) => Some(InvalidNonce(cur.toLong, sent.toLong))
      case InvalidPairNamePattern(name) => Some(InvalidPairName(Pair(name.toLowerCase)))
      case InvalidMethodPattern => Some(InvalidMethod())
      case NoOrdersPattern => Some(NoOrders())
      case NotEnoughCurrencyPattern(name) =>
        Some(NotEnoughCurrency(Currency(name.toLowerCase)))
      case PriceTooHighPattern(first, value, second) =>
        Some(PriceTooHigh(Currency(first.toLowerCase), BigDecimal(value), Currency(second.toLowerCase)))
      case VolumeTooLowPattern(first, value, second) if first == second =>
        Some(VolumeTooLow(Currency(first.toLowerCase), BigDecimal(value)))
      case _ => Some(Unknown(error))
    }
}
