import unfiltered.request._
import unfiltered.response._
import unfiltered.Cookie

import dispatch._

import dispatch.liftjson.Js._
import net.liftweb.json._

/**
 */
class App extends Templates with unfiltered.filter.Plan {
  val authorizationEndPoint = "http://localhost:8080/authorize"
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
    // if we have an access token on hand, make a user info endpoint call and parse the returned JSON
    // if not, render the current list of tokens
    case GET(Path("/") & AuthorizedToken(at)) =>
      try {
        Http(svc  / "connect" / "userinfo" <:< Map("Authorization" -> ("Bearer " + at.value)) ># { js =>
          val response = pretty(render(js))
          userInfo(response)
        })
      } catch { case e =>
        val msg = "there was an error making an api request: %s" format e.getMessage
        userInfo(msg)
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
        val postParams = Map("grant_type" -> "authorization_code", "code" -> code.get, "client_secret" -> "secret",
                             "client_id" -> client_id, "redirect_uri" -> redirect_uri)
        // Make an access token request and create a token from the returned JSON
        val accessToken = Http(svc / "token" << postParams ># { AccessToken(_) })
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
