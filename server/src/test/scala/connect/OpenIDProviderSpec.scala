package connect

import org.specs.Specification

import net.liftweb.json.JsonAST._

/**
 *
 */
class OpenIDProviderSpec extends Specification {
  val provider = new ConnectComponentRegistry().openIDProvider

  "An OpenIDProvider" should {
    "validate its own tokens" in {
      val token = provider.generateIdToken("owner", "client", Seq("openid"))

      val claims = provider.checkIdToken(token.get)

      claims \ "error" must_== JNothing
    }
  }


}
