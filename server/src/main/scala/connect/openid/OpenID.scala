package connect.openid

import unfiltered.filter.request.ContextPath
import unfiltered.oauth2.OAuthResourceOwner
import unfiltered.response._
import connect.Logger

object OpenID {
  /**
   * Additional OpenID response types (http://openid.net/specs/openid-connect-messages-1_0.html#auth_req)
   */
  val IdToken = "id_token"
  val NoResponse = "none"

}

trait UserInfoEndPoint {
  val UserInfoPath: String
}

/**
 * Session management endpoints
 */
trait SessionManagementEndPoints {
  val CheckSessionPath: String
  val RefreshSessionPath: String
  val EndSessionPath: String
}

trait DefaultUserInfoEndPoint extends UserInfoEndPoint {
  /** http://openid.net/specs/openid-connect-messages-1_0.html#userinfo_ep */
  val UserInfoPath = "/userinfo"
}

/**
 * Handles request to the OpenID connect endpoints.
 */
trait UserInfoPlan extends unfiltered.filter.Plan with UserInfoEndPoint with Logger {
  val userInfoService: UserInfoService

  import net.liftweb.json._
  import net.liftweb.json.JsonDSL._
  implicit val formats = Serialization.formats(NoTypeHints)

  def intent = {
    case req @ ContextPath(_, UserInfoPath) => req match {
      // Extract the access-token authorized user information
      case OAuthResourceOwner(id, scopes) =>

        userInfoService.userInfo(id, scopes) match {
          case Some(user) => JsonContent ~> ResponseString(Serialization.write(user))
          case None =>
            logger.error("Resource owner " + id + " is not an OpenID user")
            InternalServerError
        }

      case _ => Json(("error" -> "invalid request"))
    }

  }
}
