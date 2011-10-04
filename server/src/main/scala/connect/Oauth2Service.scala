package connect

import unfiltered.request._
import unfiltered.response._
import unfiltered.Cookie

import unfiltered.oauth2.{Client, ResourceOwner, RequestBundle}

import Templates._


trait OAuth2Service extends unfiltered.oauth2.Service {
  val ApproveKey = "Approve"
  val DenyKey = "Deny"

  def login[T](bundle: RequestBundle[T]) = loginForm(bundle)

  def requestAuthorization[T](bundle: RequestBundle[T]) = authorizationForm(bundle, ApproveKey, DenyKey)

  def invalidRedirectUri(uri: Option[String], client: Option[Client]) =
    ResponseString("missing or invalid redirect_uri")

  def resourceOwner[T](r: HttpRequest[T]): Option[User] = Authenticated.unapply(r)

  def resourceOwner(userName: String, password: String): Option[ResourceOwner] = throw new UnsupportedOperationException

  // TODO: More robust acceptance checking
  def accepted[T](r: HttpRequest[T]) = r match {
    case POST(_) & Params(p) => p("submit") match {
      case Seq(ApproveKey) => true
      case _ => false
    }
    case _ => false
  }

  def denied[T](r: HttpRequest[T]) = r match {
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
  def validScopes[T](resourceOwner: ResourceOwner, scopes: Seq[String], req: HttpRequest[T]): Boolean = true

  def invalidClient = ResponseString("invalid client")
}

/**
 * Represents the SSO session.
 * TBD: Expiry, clearing out.
 */
object SessionStore {
  val sessions = new scala.collection.mutable.HashMap[String, User]

  def newSession(u: User): String = {
    val sid = java.util.UUID.randomUUID.toString
    sessions.put(sid, u)
    sid
  }

  def get(id: String): Option[User] = sessions.get(id)

  def remove(id: String) { sessions.remove(id) }
}

/**
 * Extractor which returns the currently authenticated user.
 */
object Authenticated {
  def unapply[T](r: HttpRequest[T]): Option[User] = r match {
    case Cookies(cookies) => cookies("sid") match {
      case Some(Cookie(_, value, _, _, _, _)) => SessionStore.get(value)
      case _ =>  None
    }
  }
}

class AuthenticationPlan extends unfiltered.filter.Plan {
  def intent = {
    case Path("/") & r => index("", Authenticated.unapply(r))

    case Path("/login") & Params(p) & r =>
      r match {
        case Authenticated(user) =>
          Redirect("/")
        case _ =>
          // TODO: Select the appropriate authentication provider
          // based on ?
          (p("user"), p("password")) match {
            case (Seq(username), Seq(password)) =>
              val sid = SessionStore.newSession(User(username, None))
              ResponseCookies(Cookie("sid", sid)) ~> ((p("client_id"), p("redirect_uri"), p("response_type")) match {
                case (Seq(clientId), Seq(returnUri), Seq(responseType)) =>
                  Redirect("/authorize?client_id=%s&redirect_uri=%s&response_type=%s" format(
                    clientId, returnUri, responseType
                  ))
                case q => Redirect("/")
              })
            case _ => loginForm()
          }
      }

    case Path("/logout") & r =>
      r.cookies.find(_.name == "sid") match {
        case Some(sidCookie) => SessionStore.remove(sidCookie.value)
        case None =>
      }
      ResponseCookies(Cookie("sid","")) ~> Redirect("/")
  }
}
