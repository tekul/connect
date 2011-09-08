package crypto.cipher

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


/**
 * Encapsulates the JCE implementation of AES/CBC/PCKS5Padding.
 *
 * @author Luke Taylor
 */

object AesCipher {
  val CBC = "AES/CBC/PKCS5PADDING"
  val VALID_KEY_LENGTHS = List(16, 24, 32)

  def apply(rawKey: Array[Byte], algorithm: String = CBC): SymmetricCipher = {
    require(VALID_KEY_LENGTHS.contains(rawKey.length), "AES Key must be 128, 192 or 256 bits in length")

    new AesCipher(new SecretKeySpec(rawKey, "AES"), algorithm)
  }
}

private class AesCipher(key: SecretKey, val algorithm: String) extends SymmetricCipher {
  val requiresIv = algorithm match {
    case AesCipher.CBC => true
    case _ => throw new IllegalArgumentException("Unsupported AES algorithm: " + algorithm)
  }

  def encrypt(plaintext: Array[Byte], iv: Option[Array[Byte]]) = {
    checkIv(iv)
    CipherHelper.encrypt(algorithm, key, plaintext, iv)
  }

  def decrypt(ciphertext: Array[Byte], iv: Option[Array[Byte]]) = {
    checkIv(iv)
    CipherHelper.decrypt(algorithm, key, ciphertext, iv)
  }

  private def checkIv(iv: Option[Array[Byte]]) {
    require(!(iv == None && requiresIv), "An IV is required for " + algorithm)
  }

  def keySize = key.getEncoded.length * 8
}
