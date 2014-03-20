package com.alexknvl.btce.api

class Auth(val key: String, val secret: String) {
  import javax.crypto.Mac
  import javax.crypto.spec.SecretKeySpec
  import org.apache.commons.codec.binary.Hex

  val secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA512")
  val mac = Mac.getInstance("HmacSHA512")
  mac.init(secretKey)

  var nonce = 0: Long

  def newNonce = synchronized {
    val result = nonce
    nonce = nonce + 1
    result
  }

  def sign(data: String) = Hex.encodeHexString(mac.doFinal(data.getBytes("UTF-8")))
}
