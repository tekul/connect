package crypto.sign

import javax.crypto.spec.SecretKeySpec
import javax.crypto.{SecretKey, Mac}

/**
 * Encapsulates JCE's annoying API for using MACs.
 *
 * @author Luke Taylor
 */

object MacSigner {
  private val DEFAULT_ALGORITHM = "HMACSHA256"

  def apply(key: Array[Byte]): SignerVerifier = apply(new SecretKeySpec(key, DEFAULT_ALGORITHM))

  def apply(key: SecretKey): SignerVerifier = new MacSigner(DEFAULT_ALGORITHM, key)
}

private final class MacSigner(val algorithm: String, key: SecretKey) extends SignerVerifier {
  val keyLength = key.getEncoded.length * 8

  def sign(bytes: Array[Byte]): Array[Byte] = {
    val mac = Mac.getInstance(algorithm)
    mac.init(key)
    mac.doFinal(bytes)
  }

  override def verify(content: Array[Byte], signature: Array[Byte]) {
    val signed = sign(content)
    if (!isEqual(signed, signature)) {
      throw new InvalidSignatureException("Calculated signature did not match actual value")
    }
  }

  private def isEqual(b1: Array[Byte], b2: Array[Byte]): Boolean = {
    if (b1.length != b2.length) {
      return false
    }
    var xor = 0
    for (i <- 0 until b1.length) {
      xor |= b1(i) ^ b2(i)
    }

    xor == 0
  }
}
