package openid

import unfiltered.filter.request.ContextPath
import unfiltered.oauth2.OAuthResourceOwner
import unfiltered.response._
import net.liftweb.json.JsonDSL._

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
trait UserInfoPlan extends unfiltered.filter.Plan with UserInfoEndPoint { // with Logger {
  val userInfoService: UserInfoService

  import net.liftweb.json._
  implicit val formats = Serialization.formats(NoTypeHints)

  def intent = {
    case req @ ContextPath(_, UserInfoPath) => req match {
      // Extract the access-token authorized user information
      case OAuthResourceOwner(id, scopes) =>

        userInfoService.userInfo(id, scopes.get.split(" ")) match {
          case Some(user) => Json(Serialization.write(user))
          case None =>
//            logger.warn("Resource owner " + id + " not found in OpenID users")
            InternalServerError
        }

        // 1. get user profile from ID/scopes combination
        // 2. render as JSON or JWT

        Json(("hello" -> (id + ", " + scopes)))

      case _ => Json(("error" -> "invalid request"))
    }

  }
}
