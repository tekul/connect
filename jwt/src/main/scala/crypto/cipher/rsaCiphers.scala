package crypto.cipher

import java.security.interfaces.{RSAPublicKey, RSAPrivateKey}
import java.security.KeyFactory
import java.security.spec.{RSAPrivateKeySpec, RSAPublicKeySpec}

object RsaCipher {
  val DEFAULT_ALGORITHM = "RSA/ECB/PKCS1PADDING"

  def rsaPublicKey(n: BigInt, e: BigInt): RSAPublicKey =
    KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n.bigInteger, e.bigInteger)).asInstanceOf[RSAPublicKey]

  def rsaPrivateKey(n: BigInt, d: BigInt): RSAPrivateKey =
    KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(n.bigInteger, d.bigInteger)).asInstanceOf[RSAPrivateKey]

  private[cipher] def checkIv(iv: Option[Array[Byte]]) {
    require(iv == None, "RSA cannot be used with an initialization vector")
  }
}

import RsaCipher._

object RsaEncryptor {
  def apply(n: BigInt, e: BigInt): EncryptionCipher = apply(RsaCipher.rsaPublicKey(n, e))

  def apply(key: RSAPublicKey): EncryptionCipher = new RsaEncryptor(key, DEFAULT_ALGORITHM)
}

object RsaDecryptor {
  def apply(n: BigInt, d: BigInt): DecryptionCipher = apply(RsaCipher.rsaPrivateKey(n, d))

  def apply(key: RSAPrivateKey): DecryptionCipher = new RsaDecryptor(key, DEFAULT_ALGORITHM)
}

private abstract class AbstractRsaCipher(val algorithm: String, val keySize: Int) extends CipherMetadata

private class RsaEncryptor(key: RSAPublicKey, algorithm: String)
    extends AbstractRsaCipher(algorithm, key.getModulus.bitLength) with EncryptionCipher {

  def encrypt(plaintext: Array[Byte], iv: Option[Array[Byte]]) = {
    checkIv(iv)
    CipherHelper.encrypt(algorithm, key, plaintext, iv)
  }
}

private class RsaDecryptor(key: RSAPrivateKey, algorithm: String)
    extends AbstractRsaCipher(algorithm, key.getModulus.bitLength) with DecryptionCipher {

  def decrypt(ciphertext: Array[Byte], iv: Option[Array[Byte]]) = {
    checkIv(iv)
    CipherHelper.decrypt(algorithm, key, ciphertext, iv)
  }
}
