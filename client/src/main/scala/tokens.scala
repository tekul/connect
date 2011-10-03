import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonAST.JValue

case class RequestToken(value: String)

// TODO: Avoid storing bearer token as a cookie

object AccessToken {
  implicit val formats = DefaultFormats
  def apply(jValue: JValue) = {
    val p: Map[String, String] = jValue.extract[Map[String, String]]
    (p.get("access_token"), p.get("token_type")) match {
      case (Some(access_token), Some(token_type)) =>
        token_type match {
          case "Bearer" =>
            new AccessToken(access_token, p.get("id_token"))
          case _ => sys.error("Token type %s not recognised. Only bearer tokens are currently supported" format(token_type))
        }
      case _ => sys.error("access_token or token_type missing from token response")
    }
  }
}

case class AccessToken(value: String, idToken: Option[String])
