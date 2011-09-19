import unfiltered.request._
import unfiltered.response._
import unfiltered.Cookie

import dispatch._

import dispatch.liftjson.Js._
import net.liftweb.json._

import unfiltered.oauth2.OAuthorization._

/**
 */
class App extends Templates with unfiltered.filter.Plan {
  val authorizationEndPoint = "http://localhost:8080/oauth/authorize"
  val client_id = "exampleclient"
  val redirect_uri = "http://localhost:8081/callback"

  import QParams._

  private val svc = :/("localhost", 8080)

  private val tmap = scala.collection.mutable.Map.empty[String, AccessToken]

  object AuthorizedToken {
    def unapply[T](r: HttpRequest[T]) = r match {
      case Cookies(cookies) => cookies("token") match {
        case Some(Cookie(_, value, _, _, _, _)) => tmap.get(value)
        case _ => None
      }
    }
  }

  def intent = {
    // if we have an access token on hand, make an api call
    // if not, render the current list of tokens
    case GET(Path("/") & AuthorizedToken(at)) =>
      try {
        Http(svc / "api" / "users" / "1" <:< Map("Authorization" -> ("Bearer " + at.value)) ># { js =>
          val response = pretty(render(js))
          apiCall(response)
        })
      } catch { case e =>
        val msg = "there was an error making an api request: %s" format e.getMessage
        apiCall(msg)
      }

    // show a list of tokens, if any, and a way to connect
    case GET(Path("/")) => tokenList(tmap.values)

    case GET(Path("/disconnect")) =>
      ResponseCookies(Cookie("token", "")) ~> Redirect("/")

    // authorization callback URI
    case GET(Path("/callback") & Params(params) ) =>
      val expected = for {
        code <- lookup("code") is
          required("code is required") is nonempty("code can not be blank")
      } yield {
        val postParams = Map(GrantType -> AuthorizationCode, Code -> code.get, ClientSecret -> "secret",
                             ClientId -> client_id, RedirectURI -> redirect_uri)
        // Make an access token request and create a token from the returned JSON
        val accessToken = Http(svc / "oauth" / "token" << postParams ># { AccessToken(_) })
        println("Retrieved access token response: " + accessToken)
        val sid = java.util.UUID.randomUUID.toString
        tmap += (sid -> accessToken)
        ResponseCookies(Cookie("token", sid)) ~> Redirect("/")
      }

      expected(params) orFail { fails =>
        BadRequest ~> ResponseString(fails.map { _.error } mkString(". "))
      }

    case GET(Path("/connect")) =>
      Redirect(authorizationEndPoint + "?client_id=" + client_id + "&redirect_uri=" + redirect_uri + "&response_type=code%20id_token&scope=openid")
  }
}
