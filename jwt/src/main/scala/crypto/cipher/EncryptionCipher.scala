package crypto.cipher

/**
 * Generalization of the encryption operation of a cipher.
 *
 * @author Luke Taylor
 */

trait EncryptionCipher extends CipherMetadata {

  def encrypt(plaintext: Array[Byte], iv: Option[Array[Byte]] = None): Array[Byte]

  def encrypt(plaintext: Array[Byte], iv: Array[Byte]): Array[Byte] = {
    encrypt(plaintext, Some(iv))
  }
}
