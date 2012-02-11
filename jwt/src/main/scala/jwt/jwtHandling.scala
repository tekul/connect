package jwt

import crypto.codec.Codecs._
import crypto.sign.SignatureVerifier
import crypto.sign.Signer
import java.nio.CharBuffer

/**
 * JWT operations.
 *
 * @author Luke Taylor
 */
trait Jwt extends BinaryFormat {
  def verifySignature(verifier: SignatureVerifier)

  def claims: String

  def encoded: String
}

/**
 * Helper object for creating and manipulating JWTs.
 */
object Jwt {
  private[jwt] val PERIOD: Array[Byte] = utf8Encode(".")

  /**
   * Creates a token from an encoded token string.
   *
   * @param token the (non-null) encoded token (three Base-64 encoded strings separated by "." characters)
   */
  def apply(token: String) = {
    val firstPeriod = token.indexOf('.')
    val lastPeriod = token.lastIndexOf('.')
    require(firstPeriod > 0 && lastPeriod > firstPeriod, "JWT must have 3 tokens")
    val buffer = CharBuffer.wrap(token, 0, firstPeriod)
    // TODO: Use a Reader which supports CharBuffer
    val header = JwtHeader(buffer.toString)

    buffer.limit(lastPeriod).position(firstPeriod + 1)
    val claims = b64UrlDecode(buffer)
    val emptyCrypto = lastPeriod == token.length - 1
    val crypto = if (emptyCrypto) {
      require(header.parameters.alg == "none", "Signed or encrypted token must have non-empty crypto segment")
      new Array[Byte](0)
    } else {
      buffer.limit(token.length).position(lastPeriod + 1)
      b64UrlDecode(buffer)
    }
    new JwtImpl(header, claims, crypto);
  }

  /**
   * Create a signed token.
   *
   * @param content the body to be signed (the 'claims' segment)
   * @param signer used to sign the data, giving the "crypto" segment of the JWT.
   */
  def apply(content: CharSequence, signer: Signer) = {
    val header = JwtHeader(signer)
    val claims = utf8Encode(content)
    val crypto = signer.sign(Array.concat(b64UrlEncode(header.bytes), PERIOD, b64UrlEncode(claims)))
    new JwtImpl(header, claims, crypto)
  }
}

/**
 * Helper object for JwtHeader.
 *
 * Handles the JSON parsing and serialization.
 */
private[jwt] object JwtHeader {
  import JwtAlgorithms._
  import net.liftweb.json._
  implicit val formats = DefaultFormats

  def apply(signer: Signer) = {
    new JwtHeader(HeaderParameters(alg = sigAlg(signer.algorithm)))
  }

  def apply(header: String) = {
    val bytes = b64UrlDecode(header)
    val parameters = parse(utf8Decode(bytes)).extract[HeaderParameters]
    new JwtHeader(bytes, parameters)
  }

  def apply(alg: String, enc: String, iv: Array[Byte]): JwtHeader = {
    new JwtHeader(HeaderParameters(alg, Some(enc), Some(utf8Decode(b64UrlEncode(iv)))))
  }

  def serializeParams(params: HeaderParameters) = utf8Encode(Serialization.write(params))
}

private[jwt] case class HeaderParameters(alg: String, enc: Option[String] = None, iv: Option[String] = None)

/**
 * Header part of JWT
 *
 * @param bytes the decoded header
 * @param parameters the parameter values contained in the header
 */
private[jwt] case class JwtHeader(bytes: Array[Byte], parameters: HeaderParameters) extends BinaryFormat {
  def this(parameters: HeaderParameters) = this(JwtHeader.serializeParams(parameters), parameters)

  override def toString = utf8Decode(bytes)
}

/**
 * @param header the header, containing the JWS/JWE algorithm information.
 * @param content the base64-decoded "claims" segment (may be encrypted, depending on header information).
 * @param crypto the base64-decoded "crypto" segment.
 */
private[jwt] class JwtImpl(header: JwtHeader, content: Array[Byte], crypto: Array[Byte]) extends Jwt {

  lazy val claims = utf8Decode(content)

  /**
   * Validates a signature contained in the 'crypto' segment.
   *
   * @param verifier the signature verifier
   */
  def verifySignature(verifier: SignatureVerifier) {
    verifier.verify(signingInput, crypto)
  }

  private[jwt] def signingInput: Array[Byte] =
      Array.concat(b64UrlEncode(header.bytes), Jwt.PERIOD, b64UrlEncode(content))

  /**
   * Allows retrieval of the full token.
   *
   * @return the encoded header, claims and crypto segments concatenated with "." characters
   */
  lazy val bytes: Array[Byte] = Array.concat(b64UrlEncode(header.bytes), Jwt.PERIOD, b64UrlEncode(content), Jwt.PERIOD, b64UrlEncode(crypto))

  lazy val encoded = utf8Decode(bytes)

  override def toString = header + " " + claims + " [%s crypto bytes]".format(crypto.length)
}


