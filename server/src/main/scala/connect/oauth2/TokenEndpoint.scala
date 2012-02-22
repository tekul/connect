package connect.oauth2

import unfiltered.request._
import unfiltered.response._
import unfiltered.request.QParams._

import unfiltered.filter.request.ContextPath

import Stuff._
import OAuthorization._

/**
 */
class TokenEndpoint(clients: ClientStore, tokenStore: TokenStore) extends unfiltered.filter.Plan with Formatting {

  val InvalidRedirectURIMsg = "invalid redirect_uri"
  val UnknownClientMsg = "unknown client"

  /** @return a function which builds a
   *  response for an accept token request */
  protected def accessResponder(
    accessToken: String,
    tokenType: Option[String],
    expiresIn: Option[Int],
    refreshToken: Option[String],
    scope:Seq[String],
    extras: Iterable[(String, String)]) =
      CacheControl("no-store") ~> Pragma("no-cache") ~>
        Json(Map(AccessTokenKey -> accessToken) ++
          tokenType.map(TokenType -> _) ++
          expiresIn.map (ExpiresIn -> (_:Int).toString) ++
             refreshToken.map (RefreshToken -> _) ++
             (scope match {
               case Seq() => None
               case xs => Some(spaceEncoder(xs))
             }).map (Scope -> _) ++ extras)

  protected def errorResponder(
    error: String, desc: String,
    euri: Option[String], state: Option[String]) =
      BadRequest ~> CacheControl("no-store") ~> Pragma("no-cache") ~>
        Json(Map(Error -> error, ErrorDescription -> desc) ++
          euri.map (ErrorURI -> (_: String)) ++
          state.map (State -> _))


  def errorUri(error: String) = None

  // TODO: Extract this since it is used by both endpoints
  def validRedirectUri(provided: Option[String], client: Client): Boolean = {
    provided.isDefined && !provided.get.contains("#") && provided.get.startsWith(client.redirectUri)
  }

  def onGrantAuthCode(code: String, redirectUri: String, clientId: String, clientSecret: String) = {
    clients.client(clientId, Some(clientSecret)) match {
      case Some(client) =>
        if(!validRedirectUri(Some(redirectUri), client)) errorResponder(
          InvalidClient, "invalid redirect uri", None, None
        )
        else {
          tokenStore.token(code) match {
            case Some(token) =>
              // tokens redirectUri must be exact match to the one provided
              // in order further bind the access request to the auth request
              if (token.clientId != client.id || token.redirectUri != redirectUri)
                errorResponder(UnauthorizedClient, "client not authorized", errorUri(UnauthorizedClient), None)
              else {
                val t = tokenStore.exchangeAuthorizationCode(token)
                accessResponder(t.value, t.tokenType, t.expiresIn, t.refresh, t.scopes, t.extras)
              }
            case _ => errorResponder(InvalidRequest, "unknown code", None, None)
          }
        }
      case _ => errorResponder(InvalidRequest, UnknownClientMsg, errorUri(InvalidRequest), None)
    }
  }

  def intent = {
    case req @ POST(ContextPath(_, TokenPath)) & Params(params) =>
      val expected = for {
        grantType     <- lookup(GrantType) is required(requiredMsg(GrantType))
        code          <- lookup(Code) is optional[String, String]
        clientId      <- lookup(ClientId) is required(requiredMsg(ClientId))
        redirectURI   <- lookup(RedirectURI) is optional[String, String]
        // clientSecret is not recommended to be passed as a parameter but instead
        // encoded in a basic auth header http://tools.ietf.org/html/draft-ietf-oauth-v2-16#section-3.1
        clientSecret  <- lookup(ClientSecret) is required(requiredMsg(ClientSecret))
        refreshToken  <- lookup(RefreshToken) is optional[String, String]
        scope         <- lookup(Scope) is watch(_.map(spaceDecoder), e => "")
        userName      <- lookup(Username) is optional[String, String]
        password      <- lookup(Password) is optional[String, String]
      } yield {

        grantType.get match {
//          case ClientCredentials =>
//            onClientCredentials(clientId.get, clientSecret.get, scope.getOrElse(Nil))
//
//          case Password =>
//            (userName.get, password.get) match {
//              case (Some(u), Some(pw)) =>
//                onPassword(u, pw, clientId.get, clientSecret.get, scope.getOrElse(Nil))
//              case _ =>
//                errorResponder(
//                  InvalidRequest,
//                  (requiredMsg(Username) :: requiredMsg(Password) :: Nil).mkString(" and "),
//                  auth.errUri(InvalidRequest), None
//                )
//            }
//
//          case RefreshToken =>
//            refreshToken.get match {
//              case Some(rtoken) =>
//                onRefresh(rtoken, clientId.get, clientSecret.get, scope.getOrElse(Nil))
//              case _ => errorResponder(InvalidRequest, requiredMsg(RefreshToken), None, None)
//            }

          case AuthorizationCode =>
            (code.get, redirectURI.get) match {
              case (Some(c), Some(r)) =>
                onGrantAuthCode(c, r, clientId.get, clientSecret.get)
              case _ =>
                errorResponder(
                  InvalidRequest,
                  (requiredMsg(Code) :: requiredMsg(RedirectURI) :: Nil).mkString(" and "),
                  errorUri(InvalidRequest), None
                )
            }
          case unsupported =>
            // note the oauth2 spec does allow for extension grant types,
            // this implementation currently does not
            errorResponder(
              UnsupportedGrantType, "%s is unsupported" format unsupported,
              errorUri(UnsupportedGrantType), None)
        }
      }

    // here, we are combining requests parameters with basic authentication headers
    // the preferred way of providing client credentials is through
    // basic auth but this is not required. The following folds basic auth data
    // into the params ensuring there is no conflict in transports
       val combinedParams = (
         (Right(params): Either[String, Map[String, Seq[String]]]) /: BasicAuth.unapply(req)
       )((a,e) => e match {
         case (clientId, clientSecret) =>
           val preferred = Right(
             a.right.get ++ Map(ClientId -> Seq(clientId), ClientSecret-> Seq(clientSecret))
           )
           a.right.get(ClientId) match {
             case Seq(id) =>
               if(id == clientId) preferred else Left("client ids did not match")
             case _ => preferred
           }
         case _ => a
       })

       combinedParams fold({ err =>
         errorResponder(InvalidRequest, err, None, None)
       }, { mixed =>
         expected(mixed) orFail { errs =>
           errorResponder(InvalidRequest, errs.map { _.error }.mkString(", "), None, None)
         }
       })
  }
}
