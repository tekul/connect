package connect.oauth2

/**
 */
private[oauth2] object Stuff {
  val AuthorizePath = "/authorize"
  val TokenPath = "/token"

  implicit def s2qs(uri: String) = new {
     def ?(qs: String) =
       "%s%s%s" format(uri, if(uri.indexOf("?") > 0) "&" else "?", qs)
  }

  def spaceDecoder(raw: String) = raw.replace("""\s+"""," ").split(" ").sorted.toSeq
  def spaceEncoder(scopes: Seq[String]) = scopes.mkString("+")
  def requiredMsg(what: String) = "%s is required" format what

}

private[oauth2] trait Formatting {
  import java.net.URLEncoder
  def qstr(kvs: Iterable[(String, String)]) =
    kvs map { _ match { case (k, v) => URLEncoder.encode(k, "utf-8") + "=" + URLEncoder.encode(v, "utf-8") } } mkString("&")

  def Json(kvs: Iterable[(String, String)]) =
    unfiltered.response.ResponseString(kvs map { _ match { case (k, v) => "\"%s\":\"%s\"".format(k,v) } } mkString(
      "{",",","}"
    )) ~> unfiltered.response.JsonContent
}

object OAuth2 {
  val XAuthorizedIdentity = "X-Authorized-Identity"
  val XAuthorizedClientIdentity = "X-Authorized-Client-Identity"
  val XAuthorizedScopes = "X-Authorized-Scopes"
}

/** Extractor for a resource owner and the client they authorized, as well as the granted scope. */
object OAuthIdentity {
  import OAuth2._
  import javax.servlet.http.HttpServletRequest
  import unfiltered.request.HttpRequest

  // TODO: how can we accomplish this and not tie ourselves to underlying request?
  /**
   * @return a 3-tuple of (resource-owner-id, client-id, scopes) as an Option, or None if any of these is not available
   * in the request
   */
  def unapply[T <: HttpServletRequest](r: HttpRequest[T]): Option[(String, String, Seq[String])] =
    r.underlying.getAttribute(XAuthorizedIdentity) match {
      case null => None
      case id: String => r.underlying.getAttribute(XAuthorizedClientIdentity) match {
        case null => None
        case clientId: String => r.underlying.getAttribute(XAuthorizedScopes) match {
          case null => Some((id, clientId, Nil))
          case scopes: Seq[String] => Some((id, clientId, scopes))
        }
      }
    }
}
