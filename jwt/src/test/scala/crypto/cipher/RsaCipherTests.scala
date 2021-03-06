package crypto.cipher

import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import org.scalatest.junit.JUnitSuite
import org.junit.{Test, Before}
import crypto.codec.Codecs._

class RsaCipherTests extends JUnitSuite {
  private var publicKey: RSAPublicKey = null
  private var privateKey: RSAPrivateKey = null

  @Before
  def setUp() {
    import RsaTestKeyData._
    publicKey = RsaCipher.rsaPublicKey(N, E)
    privateKey = RsaCipher.rsaPrivateKey(N, D)
  }

  @Test
  def cipherMetadataIsCorrect() {
    val cipher = RsaEncryptor(publicKey)

    assert("RSA/ECB/PKCS1PADDING" === cipher.algorithm)
    assert(2048 === cipher.keySize)
  }

  @Test
  def encryptionAndDecryptionAreAsymmetric() {
    val pb = RsaEncryptor(publicKey)
    val pr = RsaDecryptor(privateKey)
    val plaintext = "Hi there, I'm the secret message!!!"
    val ciphertext = pb.encrypt(utf8Encode(plaintext))
    assert (plaintext === utf8Decode(pr.decrypt(ciphertext)))
  }
}
