package crypto.cipher

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import java.security.{GeneralSecurityException, Key}

import org.slf4j.LoggerFactory

import Cipher._

/**
 * Internal helper object for dealing with JCE Cipher API.
 *
 * @author Luke Taylor
 */

private[cipher] object CipherHelper {
  val logger = LoggerFactory.getLogger(getClass)

  def encrypt(algorithm: String, key: Key, plaintext: Array[Byte], spec: Option[Array[Byte]]): Array[Byte] = {
    initAndDoFinal(algorithm, ENCRYPT_MODE, key, plaintext, spec)
  }

  def decrypt(algorithm: String, key: Key, ciphertext: Array[Byte], spec: Option[Array[Byte]]): Array[Byte] = {
    initAndDoFinal(algorithm, DECRYPT_MODE, key, ciphertext, spec)
  }

  def initAndDoFinal(algorithm: String, mode: Int, key: Key, bytes: Array[Byte], spec: Option[Array[Byte]]) = {
    try {
      val cipher = Cipher.getInstance(algorithm)

      spec match {
        case Some(iv) => cipher.init(mode, key, new IvParameterSpec(iv))
        case None => cipher.init(mode, key)
      }

      cipher.doFinal(bytes)
    }
    catch {
      case e: GeneralSecurityException => {
        val message = mode match {
          case ENCRYPT_MODE => "Encryption Failed."
          case DECRYPT_MODE => "Decryption Failed."
        }
        logger.error(message, e)
        throw new RuntimeException(message, e)
      }
    }
  }

}
