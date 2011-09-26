import dispatch.{ConfiguredHttpClient, Http, Handler}
import org.apache.http.client.RedirectStrategy
import org.apache.http.impl.client.BasicCookieStore
import org.specs.Specification
import unfiltered.request.Path
import unfiltered.response.ResponseString._

class OpenIDServerSpec extends Specification with unfiltered.spec.jetty.Served {

  def setup = Server.configureServer(_)

  val authorize = host / "oauth" / "authorize"
  val token = host / "oauth" / "token"
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


  "OAuth2 requests for response_type 'code id_token'" should {
    "follow the authorization code flow" in {
      // Login first
      http(host / "login" << Map("user"->"user", "password"->"password") >|)

      val authzCodeParams = Map(
        "response_type" -> "code id_token",
        "client_id" -> "exampleclient",
        "redirect_uri" -> "http://localhost:8081/",
        "state" -> "test_state"
      )

      val approve = http(authorize <<? authzCodeParams as_str)
      approve mustMatch("A 3rd party application named")

      val headers = http(authorize << authzCodeParams + ("submit" -> "Approve") >:> { h => h })

      headers must haveKey("Location")
      val uri = new java.net.URI(headers("Location").head)

      val Code = """.*code=([^&]+).*""".r
      val State = """.*state=([^&]+).*""".r
      Code.findFirstMatchIn(uri.getQuery) must beSome
      State.findFirstMatchIn(uri.getQuery) must beSome

      println(uri.getQuery)

      uri.getQuery match {
        case State(state) => state must_== "test_state"
        case _ => fail("Missing state")
      }




    }


  }
}
