package connect

import connect.oauth2._
import connect.openid._
import unfiltered.filter.Plan

import unfiltered.request.HttpRequest

import net.liftweb.json._
import net.liftweb.json.Extraction._
import net.liftweb.json.Printer.compact
import jwt.Jwt
import crypto.sign.Signer
import crypto.sign.SignatureVerifier
import crypto.sign.RsaSigner
import crypto.sign.RsaVerifier
import connect.tokens._
import connect.boot.ComponentRegistry
import unfiltered.response.Pass

// Cake-pattern-(ish) application configuration.

/**
 * Client (relying-party) application database
 */
sealed trait ClientStoreComponent {
  def clientStore: ClientStore
}

/**
 * Token persistence
 */
trait TokenRepositoryComponent {
  def tokenRepo: TokenRepository
}

trait TokenStoreComponent { this: OpenIDProviderComponent with TokenRepositoryComponent =>

  class TokenStoreDelegator extends TokenStore with Logger {
    def refresh(other: Token): Token = tokenRepo.refresh(other)

    def token(code: String) = tokenRepo.codeToken(code)

    def refreshToken(value: String) = tokenRepo.accessTokenByRefresh(value)

    def exchangeAuthorizationCode(ct: Token) =
      tokenRepo.exchange(ct, openIDProvider.generateIdToken(ct.owner, ct.clientId, ct.scopes))

    def generateAuthorizationCode(responseTypes: Seq[String], owner: ResourceOwner, client: Client,
                          scopes: Seq[String], redirectURI: String) =
      tokenRepo.createCodeToken(responseTypes, owner, client, scopes, redirectURI)

    def generateImplicitAccessToken(responseTypes: Seq[String], owner: ResourceOwner, client: Client,
                                    scopes: Seq[String], redirectURI: String) = {
      val idToken = if (responseTypes.contains("id_token"))
        openIDProvider.generateIdToken(owner.id, client.id, scopes) else None
      tokenRepo.createAccessToken(client.id, scopes, redirectURI, owner.id, idToken)
    }

    def generateClientToken(client: Client, scopes: Seq[String]) = {
      tokenRepo.createAccessToken(client.id, scopes, client.redirectUri, client.id, None)
    }

    def generatePasswordToken(owner: ResourceOwner, client: Client, scope: Seq[String]): Token = throw new UnsupportedOperationException
  }
}

trait AccessTokenReaderComponent {
  def accessTokenReader: AccessTokenReader

  trait AccessTokenReader {
    def accessToken(value: String): Option[Token]
  }
}

/**
 * Provides token authentication for the authorization filter by validating submitted access tokens
 * against those in the token store.
 */
trait TokenAuthenticationComponent { this: AccessTokenReaderComponent with ClientStoreComponent =>
  def tokenAuthenticator: AuthorizationSource

  class StdTokenAuthenticator extends AuthorizationSource with Logger {
    def authenticateToken[T](token: AccessToken, request: HttpRequest[T]): Either[String, (ResourceOwner, String, Seq[String])] = {
      logger.debug("Authenticating token " +  token)
      token match {
        case BearerToken(value) =>
          accessTokenReader.accessToken(value) match {
            case Some(appToken) =>
              logger.debug("Found token from client %s authorized by %s" format(appToken.clientId, appToken.owner))
              clientStore.client(appToken.clientId, None) match {
                case Some(client) =>
                  Right(new User(appToken.owner, None), client.id, appToken.scopes)
                case _ =>
                  logger.debug("No client found for client id %s" format("appToken.clientId"))
                  Left("Bad token")
              }
            case None =>
              logger.debug("No matching token found")
              Left("Bad token")
          }
        case _ =>
          logger.debug("Bad token")
          Left("Bad token")
      }
    }
  }
}

trait TokenSignerComponent {
  def tokenSigner: Signer
}

trait TokenVerifierComponent {
  def tokenVerifier: SignatureVerifier
}


trait OpenIDProviderComponent { this: TokenSignerComponent with TokenVerifierComponent =>
  /**
   * The name of the OpenID Provider, which will be used in the "iss" JWT field.
   */
  def providerName: String
  def openIDProvider: OpenIDProvider

  class StdOpenIDProvider extends OpenIDProvider with Logger {
    implicit val formats = Serialization.formats(NoTypeHints)
    // TODO: create class for IdToken, as these will probably have to be cached
    /**
     * @see http://openid.net/specs/openid-connect-messages-1_0.html#id_token
     */
    def generateIdToken(owner: String, clientId: String, scopes: Seq[String]) = {
      if (scopes.contains("openid")) {
        val expiry = System.currentTimeMillis()/1000 + 600
        val claims = Map("iss" -> providerName, "user_id" -> owner, "aud" -> clientId,"exp" -> expiry)
        val jwt = Jwt(compact(render(decompose(claims))), tokenSigner)
      // Optionals iso29115, nonce, issued_to
        logger.debug("id_token is " + jwt)
        Some(jwt.encoded)
      } else None
    }

    def checkIdToken(id_token: String): JValue = {
      val jwt = Jwt(id_token)
      try {
        jwt.verifySignature(tokenVerifier)
        val claims = parse(jwt.claims)
        val ttl = (claims \ "exp") match {
          case JInt(expiry) => expiry - (System.currentTimeMillis()/1000)
          case _ => throw new IllegalStateException
        }

        if (ttl <= 0) {
          logger.debug("Token expired, ttl = " + ttl)
          invalidToken
        } else {
          logger.debug("Token expires in %ss" format ttl)
          claims
        }
      } catch {
        case e =>
          logger.debug("Token check failed", e)
          invalidToken
      }
    }

    val invalidToken: JValue = decompose(Map("error" -> "invalid_id_token"))
  }
}


/**
 * Provides `UserInfo` data for the OpenID Connect UserInfo endpoint
 */
trait UserInfoServiceComponent {
  def userInfoService: UserInfoService
}

// Web Plan Components

/**
 * Provides the OAuth2 end points
 */
trait OAuth2WebComponent {
  def oauth2Plan: Plan
}

/**
 * Handles user authentication
 */
trait AuthenticationWebComponent {
  def authenticationPlan: Plan
}

/**
 * Provides a resource protection layer for access-token validation
 */
trait TokenAuthorizationWebComponent { this: TokenAuthenticationComponent =>
  def tokenAuthorizationPlan: OAuth2Protection

  final class StdTokenAuthorizationPlan extends OAuth2Protection(tokenAuthenticator)
}

/**
 *
 */
trait UserInfoComponent { this: UserInfoServiceComponent =>
  def userInfoPlan: Plan

  class StdUserInfoPlan extends UserInfoPlan(userInfoService) with DefaultUserInfoEndPoint
}

object ServerKey {
  val n = BigInt("2524288480257888199556255116655542805598393034290055663431253555094618663541459985" +
                                "5793628180675586330233507173840470187695708486563026571606062758924232350260463016" +
                                "9736896455646506339961818059462812406492602672391849055209451282112822194179885364" +
                                "7259085005394520015198121396359091605538281352495268474472111677044833990842280912" +
                                "4910254790572161730751134504446476522022890936880873315843583964115032999480463964" +
                                "5860723499985610044885955003780473353695232907121198784095638287828200752247848835" +
                                "6382830331814602450201577977929146295540530330619043595284814844850259622787188410" +
                                "8117101706566865330917073838590369651212533")

  // Create a signer with the server's private key. Public exponent = 65537
  val e = BigInt("2494399290019089944481173041331303044590956731215317985557340303667891408969108902" +
                                "2400341099054452390744955644675262673380865424085755585452190828550165726768357499" +
                                "4618782540612530281860129382621590769441192634187841015982105901718242795936395564" +
                                "4142173185137472125734249961847675839087303945388804517681438305983686507112883804" +
                                "3246368504545178822966143073490366435436788543006846947989203708743168943752919160" +
                                "8794172532785578053879550782742434283062067883905077972676361489806170723802056790" +
                                "5244368025023098907991716567763967783960643240158687824775722357103174693411859213" +
                                "1390538277917581215983818730420391166037453")
}


class ConnectComponentRegistry extends OAuth2WebComponent with ComponentRegistry
  with AuthenticationWebComponent
  with UserInfoComponent
  with TokenAuthorizationWebComponent
  with UserInfoServiceComponent
  with OpenIDProviderComponent
  with TokenSignerComponent
  with TokenVerifierComponent
  with TokenAuthenticationComponent
  with ClientStoreComponent
  with AccessTokenReaderComponent
  with TokenStoreComponent
  with TokenRepositoryComponent {

  override val providerName = "OpenIDConnectTest"
  val authorizationEndpoint = new AuthorizationEndpoint(clientStore, new TokenStoreDelegator, new OAuth2Service)
  val tokenEndpoint = new TokenEndpoint(clientStore, new TokenStoreDelegator)
  override val oauth2Plan = new Plan {
    def intent = Pass.onPass(authorizationEndpoint.intent, tokenEndpoint.intent)
  }
  override lazy val userInfoPlan = new StdUserInfoPlan
  override lazy val tokenAuthorizationPlan = new StdTokenAuthorizationPlan
  override val authenticationPlan = new AuthenticationPlan
  override lazy val openIDProvider = new StdOpenIDProvider
  override val tokenSigner = RsaSigner(ServerKey.n, ServerKey.e)
  override val tokenVerifier = RsaVerifier(ServerKey.n, 65537)
  override lazy val clientStore = new Clients
  override lazy val tokenAuthenticator = new StdTokenAuthenticator
  override lazy val userInfoService = TestUsers
  override val tokenRepo = new InMemoryTokenRepository
  override val accessTokenReader = new AccessTokenReader() {
    def accessToken(value: String) = tokenRepo.accessToken(value)
  }

}
