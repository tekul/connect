import crypto.sign.RsaSigner
import unfiltered.jetty.{Server => JServer}
import unfiltered.request._
import unfiltered.oauth2._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Printer.compact

import jwt.Jwt

/**
 * Sample OpenID connect server, based on the OAuth2 sample at
 * https://github.com/softprops/unfiltered-oauth2-server.g8.git
 */
object Server {
  val resources = new java.net.URL(getClass.getResource("/web/robots.txt"), ".")
  val port = 8080

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
    def generateIdToken(token: Token) = {
      val expiry = System.currentTimeMillis()/1000 + 600
      val jwt = Jwt(compact(render(
      ("iss" -> "CID") ~
      ("user_id" -> token.owner) ~
      ("aud" -> token.clientId) ~
      ("exp" -> expiry)
      )), signer)
      // Optionals iso29115, nonce, issued_to

      jwt.encoded
    }
  }

  object Auth {
    lazy val authServer = new AuthorizationServer with Clients with Tokens with AppContainer {
      val openidProvider = OpenID
    }
  }

  /**
   * Used to authenticate the bearer access token from the incoming client request,
   * reading it from the TokenStore and returning the resource owner and scope information.
   */
  class MyAuthSource(tokenStore: Tokens) extends AuthSource with Logger {
    def authenticateToken[T](token: AccessToken, request: HttpRequest[T]): Either[String, (ResourceOwner, Option[String])] = {
      logger.debug("Authenticating token " +  token)
      token match {
        case BearerToken(value) =>
          tokenStore.accessToken(value) match {
            case Some(appToken) =>
              logger.debug("Found token from client %s authorized by %s" format(appToken.clientId, appToken.owner))
              // TODO: API should use Seq[String] for scopes
              Right(new User(appToken.owner, None), Some(appToken.scopes.mkString(" ")))
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

  import openid._

  object TestUsers extends UserInfoService {
    val users = Map(
      "john" -> UserProfile("john", "John Coltrane", "John", "Coltrane", Some("William"), "Trane",
        Some("http://en.wikipedia.org/wiki/John_Coltrane"),None, None, "jc@trane.jaz", false,  "male",
        Some("09/23/1926"), "America/New_York","en_US", None, Some(Address("1511 North Thirty-third Street", "Philadelphia", None, None, "USA", None)),
        "2011-09-25T23:58:42+0000."))

    override def userInfo(id: String, scopes: Seq[String]): Option[UserInfo] = {
      users.get(id)
    }
  }

  val openIDConnect = new UserInfoPlan with DefaultUserInfoEndPoint {
    val userInfoService = TestUsers
  }

  val oauth2 = new Authorized with DefaultAuthorizationPaths with DefaultValidationMessages {
    val auth = Auth.authServer
  }

  val tokenAuthorization = Protection(new MyAuthSource(Auth.authServer))

  def configureServer(server: JServer): JServer = {
    server.resources(Server.resources)
      .context("/oauth") {
        _.filter(oauth2)
      }
      .filter(Auth.authServer) // Login etc
//      .context("/openid") {
//        _.filter(tokenAuthorization)
//        .filter(openIDConnect)
//      }
      .context("/api") {
        _.filter(tokenAuthorization)
         .filter(new Api)
      }
    server
  }

  def main(args: Array[String]) {
    new java.util.Timer().schedule(new java.util.TimerTask() {
      def run() { unfiltered.util.Browser.open("http://localhost:%s/" format port) }
    }, 1000)

    configureServer(unfiltered.jetty.Http(port)).run()
  }
}
