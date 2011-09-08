package crypto.sign

import org.scalatest.junit.JUnitSuite
import org.junit.Test
import crypto.codec.{Hex, Utf8}

/**
 * @author Luke Taylor
 */

class MacSignerTests extends JUnitSuite {
  @Test
  def hmacSha256ProducesExpectedMacs() {
    // Wikipedia HMAC example.
    val mac = MacSigner(Utf8.encode("key"))
    assert("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8" === new String(Hex.encode(mac.sign(Utf8.encode("The quick brown fox jumps over the lazy dog")))))
  }
}
