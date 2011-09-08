package jwt

import crypto.cipher.RsaTestKeyData

/**
 * @author Luke Taylor
 */

object JwtSpecData {
  val HMAC_KEY: Array[Byte] = Array(3, 35, 53, 75, 43, 15, 165, 188, 131, 126, 6, 101, 119, 123, 166, 143, 90, 179, 40, 230, 240, 84, 201, 40, 169, 15, 132, 178, 210, 80, 46, 191, 211, 251, 90, 146, 210, 6, 71, 239, 150, 138, 180, 195, 119, 98, 61, 34, 61, 46, 33, 114, 5, 46, 79, 8, 192, 205, 154, 245, 103, 208, 128, 163).map(_.toByte)

  // RSA Key parts
  val N = RsaTestKeyData.N;
  val E = RsaTestKeyData.E;
  val D = RsaTestKeyData.D;
}
