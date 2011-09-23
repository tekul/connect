package crypto.sign

import org.scalatest.junit.JUnitSuite
import org.junit.Test
import crypto.codec.Codecs._

class MacSignerTests extends JUnitSuite {
  @Test
  def hmacSha256ProducesExpectedMacs() {
    // Wikipedia HMAC example.
    val mac = MacSigner(utf8Encode("key"))
    assert("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8" === new String(hexEncode(mac.sign(utf8Encode("The quick brown fox jumps over the lazy dog")))))
  }
}
