package connect.openid

import connect.Logger

import unfiltered.request._
import unfiltered.response._
import unfiltered.filter.Plan
import unfiltered.filter.request.ContextPath
import net.liftweb.json.JsonAST.JValue
import connect.oauth2.OAuthIdentity

object OpenID {
  /**
   * Additional OpenID response types (http://openid.net/specs/openid-connect-messages-1_0.html#auth_req)
   */
  val IdToken = "id_token"
  val NoResponse = "none"

}

import OpenID._

trait OpenIDProvider {
  /**
   * Creates a JWT ID token for the supplied user (resource owner)
   */
  def generateIdToken(owner: String, clientId: String, scopes: Seq[String]): Option[String]

  /**
   * Validates and decodes an `id_token` submitted to the `check_id` endpoint.
   */
  def checkIdToken(id_token: String): JValue
}


trait UserInfoEndPoint {
  val UserInfoPath: String
}

trait CheckIdEndPoint {
  val CheckIdPath: String
}

trait DefaultUserInfoEndPoint extends UserInfoEndPoint {
  /** http://openid.net/specs/openid-connect-messages-1_0.html#userinfo_ep */
  val UserInfoPath = "/userinfo"
}

trait DefaultCheckIdEndPoint extends CheckIdEndPoint {
  val CheckIdPath = "/check_id"
}

import net.liftweb.json._
import net.liftweb.json.JsonDSL._

/**
 * Handles request to the user info connect endpoint.
 */
abstract class UserInfoPlan(userInfoService: UserInfoService) extends Plan with UserInfoEndPoint with Logger {
  implicit val formats = Serialization.formats(NoTypeHints)

  def intent = {
    case req @ ContextPath(_, UserInfoPath) => req match {
      // Extract the access-token authorized user information
      case OAuthIdentity(userId, clientId, scopes)  =>
        userInfoService.userInfo(userId, scopes) match {
          case Some(user) => JsonContent ~> ResponseString(Serialization.write(user))
          case None =>
            logger.error("Resource owner " + userId + " is not an OpenID user")
            InternalServerError
        }

      case _ => Json(("error" -> "invalid request"))
    }

  }
}

abstract class CheckIdPlan(openIdProvider: OpenIDProvider) extends Plan with CheckIdEndPoint with Logger {
  implicit val formats = Serialization.formats(NoTypeHints)

  def intent = {
    case ContextPath(_, CheckIdPath) & Params(params) =>
      params(IdToken) match {
        case Seq(id_token) => Json(openIdProvider.checkIdToken(id_token))
        case Nil => Json(("error" -> "missing id_token"))
      }
  }
}
