package openid

import unfiltered.filter.request.ContextPath
import unfiltered.oauth2.OAuthResourceOwner
import unfiltered.response.{Json, ResponseString}
import net.liftweb.json.JsonDSL._

/**
 */
object OpenID {
  /**
   * Additional OpenID response types (http://openid.net/specs/openid-connect-messages-1_0.html#auth_req)
   */
  val IdToken = "id_token"
  val NoResponse = "none"

}

trait OpenIDEndPoints {
  val UserInfoPath: String
  val CheckSessionPath: String
}

trait DefaultOpenIDEndPoints extends OpenIDEndPoints {
  val UserInfoPath = "/userinfo"
  val CheckSessionPath = "/check_session"
}

/**
 * Handles request to the OpenID connect endpoints.
 */
trait OpenIDConnectPlan extends unfiltered.filter.Plan with OpenIDEndPoints {

  def intent = {
    case req @ ContextPath(_, UserInfoPath) => req match {
      case OAuthResourceOwner(id, scopes) =>
        Json(("hello" -> (id + ", " + scopes.getOrElse("no scopes supplied"))))

      case _ => Json(("error" -> "invalid request"))
    }

    case req @ ContextPath(_, CheckSessionPath) => {
      ResponseString("Check-session not implemented yet")
    }
  }
}
