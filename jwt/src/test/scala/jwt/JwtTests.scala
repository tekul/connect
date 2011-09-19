package jwt

import org.junit.Test
import org.scalatest.junit.JUnitSuite
import crypto.sign._

class JwtTests extends JUnitSuite {
  /**
   * Sample from the JWT spec.
   */
  val JOE_CLAIM_SEGMENT = "{\"iss\":\"joe\",\r\n" + " \"exp\":1300819380,\r\n" + " \"http://example.com/is_root\":true}"
  val JOE_HEADER_HMAC = "{\"typ\":\"JWT\",\r\n" + " \"alg\":\"HS256\"}"
  val JOE_HMAC_TOKEN = "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9." + "eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ." + "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
  val JOE_RSA_TOKEN = "eyJhbGciOiJSUzI1NiJ9." + "eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ." + "cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZmh7AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5jujGbds" + "9uJdbF9CUAr7t1dnZcAcQjbKBYNX4BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHlb1L07Qe7K0GarZR" + "mB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZESc6BfI7noOPqvhJ1phCnvWh6IeYI2w9QOYEUipUTI8np6LbgGY9Fs9" + "8rqVt5AXLIhWkWywlVmtVrBp0igcN_IoypGlUPQGe77Rw"
  val JOE_HEADER_RSA = "{\"alg\":\"RS256\"}"
  val hmac = MacSigner(JwtSpecData.HMAC_KEY)

  @Test def tokenBytesCreateSameToken() {
    val token = Jwt(JOE_HMAC_TOKEN)
    assert(JOE_HMAC_TOKEN === new String(token.bytes, "UTF-8"))
    assert(JOE_HMAC_TOKEN === token.encoded)
  }

  @Test def expectedClaimsValueIsReturned() {
    assert(JOE_CLAIM_SEGMENT === Jwt(JOE_HMAC_TOKEN).claims)
  }

  @Test def hmacSignedTokenParsesAndVerifies() {
    Jwt(JOE_HMAC_TOKEN).verifySignature(hmac)
  }

  @Test
  def invalidHmacSignatureRaisesException() {
    intercept[InvalidSignatureException] {
      Jwt(JOE_HMAC_TOKEN).verifySignature(MacSigner("differentkey".getBytes))
    }
  }

  @Test
  def tokenMissingSignatureIsRejected() {
    intercept[IllegalArgumentException] {
      Jwt(JOE_HMAC_TOKEN.substring(0, JOE_HMAC_TOKEN.lastIndexOf('.') + 1))
    }
  }

  @Test def hmacVerificationIsInverseOfSigning() {
    val jwt = Jwt(JOE_CLAIM_SEGMENT, hmac)
    jwt.verifySignature(hmac)
    assert(JOE_CLAIM_SEGMENT === jwt.claims)
  }

  import JwtSpecData._

  @Test
  def rsaSignedTokenParsesAndVerifies() {
    val jwt = Jwt(JOE_RSA_TOKEN)
    jwt.verifySignature(RsaVerifier(N, E))
    assert(JOE_CLAIM_SEGMENT === jwt.claims)
  }

  @Test
  def invalidRsaSignatureRaisesException() {
    val jwt = Jwt(JOE_RSA_TOKEN)
    intercept[InvalidSignatureException] {
      jwt.verifySignature(RsaVerifier(N, D))
    }
  }

  @Test def rsaVerificationIsInverseOfSigning() {
    val jwt = Jwt(JOE_CLAIM_SEGMENT, RsaSigner(N, E))
    jwt.verifySignature(RsaVerifier(N, D))
  }
}
