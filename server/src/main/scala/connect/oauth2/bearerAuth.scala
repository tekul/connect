package connect.oauth2

import unfiltered.request.{Authorization, HttpRequest}


/*
 * Authentication using bearer tokens as defined in
 *
 * http://tools.ietf.org/html/draft-ietf-oauth-v2-bearer-15
 */

/**
 * Extractor for the bearer header HTTP authentication scheme
 */
object BearerAuth {

  object BearerHeader {
    val HeaderPattern = """Bearer ([\w\d!#$%&'\(\)\*+\-\.\/:<=>?@\[\]^_`{|}~\\,;]+)""".r

    def unapply(hval: String) = hval match {
      case HeaderPattern(token) => Some(token)
      case _ => None
    }
  }

  def unapply[T](r: HttpRequest[T]) = r match {
    case Authorization(BearerHeader(token)) => Some(token)
    case _ => None
  }
}

/**
 * Authentication using a form-encoded access_token parameter
 * in the request body.
 *
 * http://tools.ietf.org/html/draft-ietf-oauth-v2-bearer-14#section-2.2
 */
object ParamTokenAuth {
  def unapply[T](r: HttpRequest[T]) = r.parameterValues(OAuthorization.AccessTokenKey) match {
    case Seq(token) => Some(token)
    case _ => None
  }
}
