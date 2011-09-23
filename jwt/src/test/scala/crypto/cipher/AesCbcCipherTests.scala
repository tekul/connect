package crypto.cipher

import java.util.Arrays
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import crypto.codec.Codecs._

/**
 * @author Luke Taylor
 */

class AesCbcCipherTests extends JUnitSuite {

  @Test
  def cipherMetadataIsCorrect() {
    val cipher = AesCipher(new Array[Byte](32))
    assert(256 === cipher.keySize)
    assert("AES/CBC/PKCS5PADDING" === cipher.algorithm)
  }

  @Test
  def invalidKeySizeIsRejected() {
    intercept[IllegalArgumentException] {
      AesCipher(new Array[Byte](31))
    }
  }

  @Test
  def encryptedPlaintextDecryptsToOriginalValue() {
    val key = new Array[Byte](32)
    val iv = new Array[Byte](16)
    Arrays.fill(key, 0xbf.asInstanceOf[Byte])
    Arrays.fill(iv, 37.asInstanceOf[Byte])
    val cipher = AesCipher(key)
    val plaintext = "Hi there, I'm the secret message!!!"
    val ciphertext = cipher.encrypt(utf8Encode(plaintext), iv)

    assert(plaintext === utf8Decode(cipher.decrypt(ciphertext, iv)))
  }
}
