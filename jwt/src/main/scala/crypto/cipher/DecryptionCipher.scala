package crypto.cipher

/**
 * Generalization of the decryption operation of a cipher.
 *
 * @author Luke Taylor
 */

trait DecryptionCipher extends CipherMetadata {

  def decrypt(ciphertext: Array[Byte], iv: Option[Array[Byte]] = None): Array[Byte]

  def decrypt(ciphertext: Array[Byte], iv: Array[Byte]): Array[Byte] = {
    decrypt(ciphertext, Some(iv))
  }
}
