package connect

import crypto.sign.RsaSigner
import openid.{UserInfo, UserProfile, UserInfoService}
import unfiltered.jetty.Server
import unfiltered.request._
import unfiltered.oauth2._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Printer.compact

import jwt.Jwt
import net.liftweb.json._
import unfiltered.filter.Plan

class TokenAuthorization extends Protection(new MyAuthSource(Auth.authServer))

class MyAuthSource(tokenStore: Tokens) extends AuthSource with Logger {
  def authenticateToken[T](token: AccessToken, request: HttpRequest[T]): Either[String, (ResourceOwner, Seq[String])] = {
    logger.debug("Authenticating token " +  token)
    token match {
      case BearerToken(value) =>
        tokenStore.accessToken(value) match {
          case Some(appToken) =>
            logger.debug("Found token from client %s authorized by %s" format(appToken.clientId, appToken.owner))
            Right(new User(appToken.owner, None), appToken.scopes)
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

object OpenID extends OpenIDProvider {
  // Create a signer with the server's private key. Public exponent = 65537
  val signer = RsaSigner(BigInt("2524288480257888199556255116655542805598393034290055663431253555094618663541459985" +
                                "5793628180675586330233507173840470187695708486563026571606062758924232350260463016" +
                                "9736896455646506339961818059462812406492602672391849055209451282112822194179885364" +
                                "7259085005394520015198121396359091605538281352495268474472111677044833990842280912" +
                                "4910254790572161730751134504446476522022890936880873315843583964115032999480463964" +
                                "5860723499985610044885955003780473353695232907121198784095638287828200752247848835" +
                                "6382830331814602450201577977929146295540530330619043595284814844850259622787188410" +
                                "8117101706566865330917073838590369651212533"),
                         BigInt("2494399290019089944481173041331303044590956731215317985557340303667891408969108902" +
                                "2400341099054452390744955644675262673380865424085755585452190828550165726768357499" +
                                "4618782540612530281860129382621590769441192634187841015982105901718242795936395564" +
                                "4142173185137472125734249961847675839087303945388804517681438305983686507112883804" +
                                "3246368504545178822966143073490366435436788543006846947989203708743168943752919160" +
                                "8794172532785578053879550782742434283062067883905077972676361489806170723802056790" +
                                "5244368025023098907991716567763967783960643240158687824775722357103174693411859213" +
                                "1390538277917581215983818730420391166037453"))

  // TODO: create class for IdToken, as these will probably have to be cached
  /**
   * @see http://openid.net/specs/openid-connect-messages-1_0.html#id_token
   */
  def generateIdToken(owner: String, clientId: String, scopes: Seq[String]) = {
    val expiry = System.currentTimeMillis()/1000 + 600
    val jwt = Jwt(compact(render(
    ("iss" -> "CFID") ~
    ("user_id" -> owner) ~
    ("aud" -> clientId) ~
    ("exp" -> expiry)
    )), signer)
    // Optionals iso29115, nonce, issued_to

    jwt.encoded
  }
}


object Auth {
  lazy val authServer = new AuthorizationServer with Clients with Tokens with OAuth2Service {
    val openidProvider = OpenID
  }
}

class UserInfoFilter extends openid.UserInfoPlan with openid.DefaultUserInfoEndPoint {
  val userInfoService = TestUsers
}

class OAuth2Filter extends Authorized with DefaultAuthorizationPaths with DefaultValidationMessages {
  val auth = Auth.authServer
}

/**
 * Sample OpenID connect server, based on the OAuth2 sample at
 * https://github.com/softprops/unfiltered-oauth2-server.g8.git
 */
object ConnectServer {
  val resources = new java.net.URL(getClass.getResource("/src/main/webapp/robots.txt"), ".")
  val port = 8080

  val tokenAuthorization = new TokenAuthorization

  /**
   * Used to authenticate the bearer access token from the incoming client request,
   * reading it from the TokenStore and returning the resource owner and scope information.
   */
  def configureServer(server: Server): Server = {
    server.resources(ConnectServer.resources)
      .filter(new OAuth2Filter)
      .filter(new AuthenticationPlan) // Login etc
      .filter(tokenAuthorization)
      .filter(new UserInfoFilter)
//      .context("/api") {
//        _.filter(tokenAuthorization)
//         .filter(new Api)
//      }
    server
  }

  def main(args: Array[String]) {
    new java.util.Timer().schedule(new java.util.TimerTask() {
      def run() { unfiltered.util.Browser.open("http://localhost:%s/" format port) }
    }, 1000)

    configureServer(unfiltered.jetty.Http(port)).run()
  }
}
