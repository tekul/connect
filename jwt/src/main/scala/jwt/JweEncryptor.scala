package jwt

import java.security.SecureRandom
import crypto.cipher.{AesCipher, EncryptionCipher}
import JwtAlgorithms._
import crypto.codec.Codecs._

/**
 * Encrypts JWTs using JWE-defined hybrid-encryption algorithms.
 *
 */
trait JweEncryptor {
  def encrypt(content: Array[Byte]): Jwt

  def encrypt(content: BinaryFormat): Jwt = {
    encrypt(content.bytes)
  }

  def encrypt(content: CharSequence): Jwt = {
    encrypt(utf8Encode(content))
  }
}

object JweEncryptor {

  /**
   * Creates a standard JweEncryptor which uses a standard AES cipher to encrypt the content
   * and uses the supplied key-encryption cipher (typically RSA) to encrypt the generated 256-bit AES key.
   */
  def apply(keyCipher: EncryptionCipher): JweEncryptor = {
    new StandardJweEncryptor(keyCipher)
  }
}

private class StandardJweEncryptor(keyCipher: EncryptionCipher) extends JweEncryptor {
  val alg = keyEncryptionAlg(keyCipher.algorithm)

  def encrypt(content: Array[Byte]): Jwt = {
    val random = new SecureRandom
    val key = new Array[Byte](32)
    random.nextBytes(key)
    val cipher = AesCipher(key)
    val iv = new Array[Byte](16)
    random.nextBytes(iv)

    val ciphertext = cipher.encrypt(content, Some(iv))
    val encryptedKey = keyCipher.encrypt(key)

    new JwtImpl(JwtHeader(alg, enc(cipher), b64UrlEncode(iv)), ciphertext, encryptedKey)
  }
}
