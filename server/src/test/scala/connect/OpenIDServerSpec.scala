package connect

import scala.util.matching.Regex
import dispatch.{ConfiguredHttpClient, Http, Handler}
import org.apache.http.client.RedirectStrategy
import org.apache.http.impl.client.BasicCookieStore
import org.specs.Specification
import jwt.Jwt

class OpenIDServerSpec extends Specification with unfiltered.spec.jetty.Served {

  object Config extends ConnectComponentRegistry

  def setup = _.filter(Config.oauth2Plan)
                .filter(Config.authenticationPlan)
                .filter(Config.tokenAuthorizationPlan)
                .filter(Config.userInfoPlan)


  val authorize = host / "authorize"
  val token = host / "token"
  val userInfo = host / "userinfo"
  val cookies = new BasicCookieStore

  override def http[T](handler: Handler[T]): T = {
    val h = new Http {
      override def make_client = {
        val c = new ConfiguredHttpClient(credentials)
        c.setCookieStore(cookies)
        c.setRedirectStrategy(new RedirectStrategy {
          import org.apache.http.protocol.HttpContext
          import org.apache.http.{HttpRequest=> HcRequest,HttpResponse=>HcResponse}
          def getRedirect(req: HcRequest, res: HcResponse, ctx: HttpContext) = null
          def isRedirected(request: HcRequest, response: HcResponse, context: HttpContext) = false
        })
       c
      }
    }
    try { h.x(handler) }
    finally { h.shutdown() }
  }

  "OAuth2 requests for response_type 'code'" should {
    "follow the authorization code flow" in {

      val client = AppClient("exampleclient", "secret", "http://localhost:8081/")

      // Login first
      http(host / "login" << Map("user"->"john", "password"->"password") >|)

      val authzCodeParams = Map(
        "response_type" -> "code",
        "client_id" -> client.id,
        "redirect_uri" -> client.redirectUri,
        "state" -> "test_state",
        "scope" -> "openid"
      )

      // Send the authorization request
      val approve = http(authorize <<? authzCodeParams as_str)
      approve mustMatch("A 3rd party application named")

      // Post back the approval form
      val authzHdrs = http(authorize << authzCodeParams + ("submit" -> "Approve") >:> { h => h })

      // Expect redirect to client with an authorization code and the original state value
      authzHdrs must haveKey("Location")
      val uri = new java.net.URI(authzHdrs("Location").head)

      val CodeState = new Regex(""".*code=([^&]+).*state=([^&]+)""")

      val code = uri.getQuery match {
        case CodeState(c, s) =>
          s must_== "test_state"
          c
        case _ => fail("code or state params missing")
      }

      // Send the access token request, authenticating as the client
      val req = token << Map(
          "grant_type" -> "authorization_code",
          "client_id" -> client.id,
          "redirect_uri" -> client.redirectUri,
          "code" -> code
        ) as_!(client.id, client.secret)

      import dispatch.liftjson.Js._
      import net.liftweb.json._
      implicit val formats = DefaultFormats

      val (accessToken, idToken) = http(req ># { js =>
          val params = js.extract[Map[String, String]]
          params must haveKey("access_token")
          params must haveKey("id_token")
          (params("access_token"), params("id_token"))
        })

      // Decode the idToken claims
      val claims = parse(Jwt(idToken).claims).extract[Map[String,String]]

      // Client posts request to the user info endpoint with the access token

      val uinfo = http(userInfo <:< Map("Authorization" -> ("Bearer " + accessToken)) as_str)
      // TODO: Check response header for content type json/jwt

      parse(uinfo)

      println(uinfo)
    }
  }
}
