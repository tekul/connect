package connect

import org.specs.Specification

import net.liftweb.json.JsonAST._

/**
 *
 */
class OpenIDProviderSpec extends Specification {
  val provider = OpenID

  "An OpenIDProvider" should {
    "validate its own tokens" in {
      val token = provider.generateIdToken("owner", "client", Seq("openid"))

      val claims = provider.checkIdToken(token)

      claims \ "error" must_== JNothing
    }
  }


}
