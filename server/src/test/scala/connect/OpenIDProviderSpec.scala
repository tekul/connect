package connect

import org.specs.Specification

/**
 *
 */
class OpenIDProviderSpec extends Specification {
  val provider = OpenID

  "An OpenIDProvider" should {
    "validate its own tokens" in {
      val token = provider.generateIdToken("owner", "client", Seq("openid"))

      provider.checkIdToken(token)
    }
  }


}
