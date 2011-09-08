package crypto.sign

import java.security.Signature
import java.security.interfaces.{RSAPublicKey, RSAPrivateKey}
import crypto.cipher.RsaCipher

/**
 *  Creates a signer for signing using an RSA private key.
 */
object RsaSigner {
  val DEFAULT_ALGORITHM = "SHA256withRSA"

  def apply(n: BigInt, d: BigInt): Signer = apply(RsaCipher.rsaPrivateKey(n, d))

  def apply(key: RSAPrivateKey): Signer = new RsaSigner(DEFAULT_ALGORITHM, key)
}

object RsaVerifier {
  def apply(n: BigInt, e: BigInt): SignatureVerifier = apply(RsaCipher.rsaPublicKey(n, e))

  def apply(key: RSAPublicKey): SignatureVerifier = new RsaVerifier(RsaSigner.DEFAULT_ALGORITHM, key)
}

private class RsaSigner(val algorithm: String, key: RSAPrivateKey) extends Signer {

  def sign(bytes: Array[Byte]) = {
    val signature = Signature.getInstance(algorithm)
    signature.initSign(key)
    signature.update(bytes)
    signature.sign
  }
}

private class RsaVerifier(val algorithm: String, key: RSAPublicKey) extends SignatureVerifier {

  def verify(content: Array[Byte], sig: Array[Byte]) {
    val signature = Signature.getInstance(algorithm)
    signature.initVerify(key)
    signature.update(content)

    if (!signature.verify(sig)) {
      throw new InvalidSignatureException("RSA Signature did not match content")
    }
  }
}
