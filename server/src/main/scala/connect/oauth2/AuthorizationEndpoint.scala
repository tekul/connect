package connect.oauth2

import unfiltered.request._
import unfiltered.response._
import unfiltered.filter.request.ContextPath
import unfiltered.request.QParams._
import unfiltered.response.Redirect

import OAuthorization._
import Stuff._

/**
 *
 * http://tools.ietf.org/html/draft-ietf-oauth-v2-23#section-3.1
 */
class AuthorizationEndpoint(clients: ClientStore, tokenStore: TokenStore, service: Service) extends unfiltered.filter.Plan with Formatting {

  /** Some servers may wish to override this with custom redirect_url
   *  validation rules. We are being lenient here by checking the base
   *  of the registered redirect_uri. The spec recommends using the `state`
   *  param for per-request customization.
   * @return true if valid, false otherwise
   * see http://tools.ietf.org/html/draft-ietf-oauth-v2-22#section-3.1.2.2
   *
   * TODO: See TokenEndpoint
   */
  def validRedirectUri(provided: Option[String], client: Client): Boolean =
    provided.isDefined && !provided.get.contains("#") && provided.get.startsWith(client.redirectUri)

  def validScopes(scopes: Seq[String]) = true // TODO: Where should this go? Register scopes? OpenID ?

  def errorResponse(redirectUri: String, error: String, desc: String,
      euri: Option[String], state: Option[String], frag: Boolean): ResponseFunction[Any] = {
    val params = Map(Error -> error, ErrorDescription -> desc) ++
          euri.map(ErrorURI -> (_:String)) ++
          state.map(State -> _)

    if (frag) {
      Redirect("%s#%s" format(redirectUri, qstr(params)))
    } else {
      Redirect(redirectUri ? qstr(params))
    }
  }

  /**
   */
  def onAuthorizationRequest[T](req: HttpRequest[T], responseType: Seq[String], clientId: String,
      redirectURI: Option[String], scope: Seq[String], state: Option[String]): ResponseFunction[Any] = {

    // All responses are fragment encoded except "code"
    val frag = !responseType.equals(Seq(Code))

    clients.client(clientId, None) match {
      case Some(client) =>
        if(!validRedirectUri(redirectURI, client))
          service.invalidRedirectUri(redirectURI, Some(client))
        else if(!validScopes(scope)) {
          errorResponse(redirectURI.get, InvalidScope, "invalid scope", service.errorUri(InvalidScope), state, frag)
        } else {
          service.resourceOwner(req) match {
            case Some(owner) =>
               if (service.denied(req))
                 errorResponse (redirectURI.get, AccessDenied, "user denied request", service.errorUri(AccessDenied), state, frag)
               else if (service.accepted(req)) {
                 responseType match {
                   case Seq(Code) =>
                     // Authorization code flow
                     val code = tokenStore.generateAuthorizationCode(responseType, owner, client, scope, redirectURI.get)

                     Redirect(redirectURI.get ? "code=%s%s".format(code, state.map("&state=%s".format(_)).getOrElse("")))

                   case Seq(TokenKey) =>
                     // Implicit flow
                     val t = tokenStore.generateImplicitAccessToken(responseType, owner, client, scope, redirectURI.get)

                     // TODO: Get the different token types to return the response parameters here and combine them into
                     // a qstr

                     val fragment = qstr(
                       Map(AccessTokenKey -> t.value) ++
                         t.tokenType.map(TokenType -> _) ++
                         t.expiresIn.map(ExpiresIn -> (_:Int).toString) ++
                         (t.scopes match {
                           case Seq() => None
                           case xs => Some(spaceEncoder(xs))
                         }).map(Scope -> _) ++
                         state.map(State -> _) ++ t.extras
                       )
                     Redirect("%s#%s" format(redirectURI.get, fragment))

                   // OpenID Cases?
// http://openid.net/specs/oauth-v2-multiple-response-types-1_0.html
                   case Seq("code", "id_token") =>
                     ResponseString("'code id_token' not implemented yet")
//                         When supplied as the value for the response_type parameter, a successful response MUST include both an Authorization Code as well as an id_token. Both success and error responses SHOULD be fragment-encoded.
                   case Seq("code", "token") =>
//                         When supplied as the value for the response_type parameter, a successful response MUST include both an Access Token and an Authorization Code as defined in the OAuth 2.0 specification. Both successful and error responses SHOULD be fragment-encoded.
                     ResponseString("'code token' not implemented yet")
                   case Seq("id_token", "token") =>
//                         When supplied as the value for the response_type parameter, a successful response MUST include both an Access Token as well as an id_token. Both success and error responses SHOULD be fragment-encoded.
                     ResponseString("'id_token token' not implemented yet")
                   case Seq("code", "id_token", "token") =>
                     ResponseString("'code id_token token' not implemented yet")
//                         When supplied as the value for the response_type parameter, a successful response MUST include an Authorization Code, an id_token, and an Access Token. Both success and error responses SHOULD be fragment-encoded.
                   case unknown =>
                     errorResponse(redirectURI.get, UnsupportedResponseType, "unsupported response type(s) %s" format unknown,
                                   service.errorUri(UnsupportedResponseType), state, frag)
                 }
               } else {
                  service.requestAuthorization(
                     RequestBundle(req, responseType, client, Some(owner), redirectURI.get, scope, state)
                   )
               }
            case _ =>
              service.login(RequestBundle(req, responseType, client, None, redirectURI.get, scope, state))
          }
        }
      case _ => service.invalidClient
    }
  }

  def intent = {
    case req @ ContextPath(_, AuthorizePath) & Params(params) =>
      val expected = for {
        responseType <- lookup(ResponseType) is required(requiredMsg(ResponseType)) is
                          watch(_.map(spaceDecoder), e => "")
        clientId     <- lookup(ClientId) is required(requiredMsg(ClientId))
        redirectURI  <- lookup(RedirectURI) is required(requiredMsg(RedirectURI))
        scope        <- lookup(Scope) is watch(_.map(spaceDecoder), e => "")
        state        <- lookup(State) is optional[String, String]
      } yield {
        onAuthorizationRequest(req, responseType.get, clientId.get, redirectURI, scope.getOrElse(Nil), state.get)
      }

      expected(params) orFail { errs =>
        params(RedirectURI) match {
          case Seq(uri) =>
            val qs = qstr(Map(
              Error -> InvalidRequest,
              ErrorDescription -> errs.map { _.error }.mkString(", ")
            ))
            params(ResponseType) match {
              case Seq(Code) =>
                Redirect(uri ? qs)
              case _ =>
                Redirect("%s#%s" format(uri, qs))
            }
          case _ =>
            ResponseString("missing or invalid redirect_uri")
        }
      }
  }
}
