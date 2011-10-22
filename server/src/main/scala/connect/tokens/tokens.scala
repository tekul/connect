package connect.tokens
import unfiltered.oauth2._
import connect.Logger


case class AppToken(value: String, clientId: String, scopes: Seq[String],
                  redirectUri: String, owner: String, refresh: Option[String], idToken: Option[String]) extends Token {
  def expiresIn = Some(3600)
  def tokenType = "Bearer"
  override val extras = idToken match {
    case Some(idt) => Map("id_token" -> idt)
    case None => Map.empty[String, String]
  }
}

/**
 * Token that represents an Authorization code request, allowing client Id, etc to be cached
 */
case class CodeToken(responseTypes: Seq[String], value: String, clientId: String, scopes: Seq[String],
                    redirectUri: String, owner: String) extends Token {

  def tokenType = throw new UnsupportedOperationException
  def refresh = throw new UnsupportedOperationException
  def expiresIn = throw new UnsupportedOperationException
  def idToken = throw new UnsupportedOperationException
}

trait TokenRepository {

  def codeToken(value: String): Option[Token]

  def accessToken(value: String): Option[Token]

  def accessTokenByRefresh(value: String): Option[Token]

  def exchange(codeToken: Token, idToken: Option[String]): Token

  def createCodeToken(responseTypes: Seq[String], owner: ResourceOwner, client: Client,
      scopes: Seq[String], redirectURI: String): String

  def createAccessToken(clientId: String , scopes: Seq[String], redirectUri: String, owner: String, idToken: Option[String]): Token

  def refresh(t: Token): Token
}

class InMemoryTokenRepository extends TokenRepository with Logger {
  import java.util.UUID.randomUUID
  import scala.collection.mutable.HashMap

  private val accessTokens = new HashMap[String,AppToken]
  private val codeTokens = new HashMap[String,CodeToken]

  def codeToken(value: String) = codeTokens.remove(value)

  def accessToken(value: String) = accessTokens.get(value)

  def exchange(ct: Token, idToken: Option[String]): Token = {
    logger.debug("Exchanging authorization code token: " + ct)
    createAccessToken(ct.clientId, ct.scopes, ct.redirectUri, ct.owner, idToken)
  }

  def createCodeToken(responseTypes: Seq[String], owner: ResourceOwner, client: Client,
      scopes: Seq[String], redirectURI: String): String = {
    val ct = CodeToken(responseTypes, randomUUID.toString, client.id, scopes, redirectURI, owner.id)
    codeTokens.put(ct.value, ct)
    ct.value
  }

  def createAccessToken(clientId: String, scopes: Seq[String], redirectUri: String,
      owner: String, idToken: Option[String]): Token = {
    val at = AppToken(randomUUID.toString, clientId, scopes, redirectUri, owner, Some("refreshToken"), idToken)
    accessTokens.put(at.value, at)
    at
  }

  def accessTokenByRefresh(value: String): Option[Token] =
    accessTokens.values.filter(_.refresh.get == value).headOption

  def refresh(t: Token) = {
    logger.debug("Refreshing access token: " + t)
    accessTokens.remove(t.value)
    createAccessToken(t.clientId, t.scopes, t.redirectUri, t.owner, t.asInstanceOf[AppToken].idToken)
  }
}
