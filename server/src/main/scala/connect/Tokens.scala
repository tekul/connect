package connect

import unfiltered.oauth2.{Client, ResourceOwner, Token, TokenStore}

case class AppToken(value: String, clientId: String, scopes: Seq[String],
                    redirectUri: String, owner: String, idToken: Option[String]=None)
     extends Token {
       def refresh = Some("refreshToken")
       def expiresIn = Some(3600)
  // We only support bearer tokens for access tokens. TODO: Authorization tokens should not support this
       def tokenType = "bearer"
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

trait OpenIDProvider {
  /**
   * Creates a JWT ID token for the supplied user (resource owner)
   */
  def generateIdToken(token: Token): String
}

trait Tokens extends TokenStore with Logger {
  import scala.collection.JavaConversions._
  import java.util.UUID.randomUUID
  private val accessTokens = new java.util.HashMap[String, AppToken]
  private val codeTokens = new java.util.HashMap[String, CodeToken]

  val openidProvider: OpenIDProvider

  /** here you would normally be generating a new token to
   *  replace an existing access token */
  def refresh(other: Token): Token = AppToken(
    other.value, other.clientId, other.scopes, other.redirectUri, other.owner
  )

  /** may want to rename to this authorizationCode */
  def token(code: String) = codeTokens.get(code) match {
    case null => None
    case t => Some(t)
  }

  def accessToken(value: String) = accessTokens.get(value) match {
    case null => None
    case t => Some(t)
  }

  def refreshToken(refreshToken: String) =
    accessTokens.values().filter(_.refresh.get==refreshToken).headOption

  def exchangeAuthorizationCode(other: Token) = {
    val ct = other.asInstanceOf[CodeToken]

    logger.debug("Exchanging authorization code token: " + ct)

    val idToken = if (ct.responseTypes.contains("id_token")) Some(openidProvider.generateIdToken(ct)) else None

    logger.debug("id_token is " + idToken)

    val at = AppToken(randomUUID.toString, other.clientId, other.scopes, other.redirectUri, other.owner, idToken)
    accessTokens.put(at.value, at)
    at
  }

  def generateAuthorizationCode(responseTypes: Seq[String], owner: ResourceOwner, client: Client,
                        scopes: Seq[String], redirectURI: String) = {
    val ct = CodeToken(responseTypes, randomUUID.toString, client.id, scopes, redirectURI, owner.id)
    codeTokens.put(ct.value, ct)
    ct.value
  }

  /** these tokens are not associated with a resource owner */
  def generateClientToken(client: Client, scopes: Seq[String]) = {
    val at = AppToken(randomUUID.toString, client.id, scopes, client.redirectUri, client.id)
    accessTokens.put(at.value, at)
    at
  }

  def generateImplicitAccessToken(responseTypes: Seq[String], owner: ResourceOwner, client: Client,
                                  scopes: Seq[String], redirectURI: String) = {
    val at = AppToken(randomUUID.toString, client.id, scopes, redirectURI, owner.id)
    accessTokens.put(at.value, at)
    at
  }

  def generatePasswordToken(owner: ResourceOwner, client: Client, scope: Seq[String]): Token = throw new UnsupportedOperationException
}
