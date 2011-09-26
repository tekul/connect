import unfiltered.request._
import unfiltered.response._
import unfiltered.Cookie

import unfiltered.oauth2.{Client, ResourceOwner, RequestBundle}

trait AppContainer extends unfiltered.filter.Plan with unfiltered.oauth2.Service with Templates {
  import scala.collection.JavaConversions._
  import unfiltered.request.{HttpRequest => Req}

  val sessions = new java.util.HashMap[String, User]

  val ApproveKey = "Approve"
  val DenyKey = "Deny"

  def login[T](bundle: RequestBundle[T]) = loginForm(bundle)

  def requestAuthorization[T](bundle: RequestBundle[T]) = authorizationForm(bundle, ApproveKey, DenyKey)

  def invalidRedirectUri(uri: Option[String], client: Option[Client]) =
    ResponseString("missing or invalid redirect_uri")

  def resourceOwner[T](r: Req[T]): Option[User] = r match {
    case Cookies(cookies) => cookies("sid") match {
      case Some(Cookie(_, value, _, _, _, _)) => sessions.get(value) match {
        case null => None
        case u => Some(u)
      }
      case _ =>  None
    }
  }

  def resourceOwner(userName: String, password: String): Option[ResourceOwner] = throw new UnsupportedOperationException

  // TODO: More robust acceptance checking
  def accepted[T](r: Req[T]) = r match {
    case POST(_) & Params(p) => p("submit") match {
      case Seq(ApproveKey) => true
      case _ => false
    }
    case _ => false
  }

  def denied[T](r: Req[T]) = r match {
    case POST(_) & Params(p) => p("submit") match {
      case Seq(DenyKey) => true
      case _ => false
    }
    case _ => false
  }


  def errorUri(err: String) = None

  def validScopes(scopes: Seq[String]) = true

  /**
   * TODO: Support for checking client/resource owner scopes.
   */
  def validScopes[T](resourceOwner: ResourceOwner, scopes: Seq[String], req: Req[T]): Boolean = true

  def invalidClient = ResponseString("invalid client")

  def intent = {
    case Path("/") & r => index("", resourceOwner(r))

    case Path("/login") & Params(p) & r =>
      resourceOwner(r) match {
        case Some(u) =>
          Redirect("/")
        case _ =>
          (p("user"), p("password")) match {
            case (Seq(username), Seq(password)) =>
              val u = User(username, None)
              val sid = java.util.UUID.randomUUID.toString
              sessions.put(sid, u)
              ResponseCookies(Cookie("sid", sid)) ~> ((p("client_id"), p("redirect_uri"), p("response_type")) match {
                case (Seq(clientId), Seq(returnUri), Seq(responseType)) =>
                  Redirect("/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=%s" format(
                    clientId, returnUri, responseType
                  ))
                case q => Redirect("/")
              })
            case _ => loginForm()
          }
      }

    case Path("/logout") & r =>
      r.cookies.find(_.name == "sid") match {
        case Some(sidCookie) => sessions.remove(sidCookie)
        case None =>
      }
      ResponseCookies(Cookie("sid","")) ~> Redirect("/")
  }
}
