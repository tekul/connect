package jwt

import crypto.codec.Base64
import crypto.codec.Utf8
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
}

/**
 * Helper object for creating and manipulating JWTs.
 */
object Jwt {
  private[jwt] val PERIOD: Array[Byte] = Utf8.encode(".")

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
    val claims = Base64.urlDecode(buffer)
    val emptyCrypto = lastPeriod == token.length - 1
    val crypto = if (emptyCrypto) {
      require(header.parameters.alg == "none", "Signed or encrypted token must have non-empty crypto segment")
      new Array[Byte](0)
    } else {
      buffer.limit(token.length).position(lastPeriod + 1)
      Base64.urlDecode(buffer)
    }
    new JwtImpl(header, claims, crypto);
  }

  /**
   * Create a signed token.
   *
   * @param content the body to be signed (the 'claims' segment)
   * @param alg the alg header parameter (must be a signature algorithm, or 'none')
   */
  def apply(content: CharSequence, signer: Signer) = {
    val header = JwtHeader(signer)
    val claims = Utf8.encode(content)
    val crypto = signer.sign(Array.concat(header.bytes, PERIOD, Base64.urlEncode(claims)))
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
    val bytes = Utf8.encode(header)
    val parameters = parse(Utf8.decode(Base64.decode(bytes))).extract[HeaderParameters]
    new JwtHeader(bytes, parameters)
  }

  def apply(alg: String, enc: String, iv: Array[Byte]): JwtHeader = {
    new JwtHeader(HeaderParameters(alg, Some(enc), Some(Utf8.decode(Base64.urlEncode(iv)))))
  }

  def serializeParams(params: HeaderParameters) = Base64.urlEncode(Serialization.write(params))
}

private[jwt] case class HeaderParameters(alg: String, enc: Option[String] = None, iv: Option[String] = None)

private[jwt] case class JwtHeader(bytes: Array[Byte], parameters: HeaderParameters) extends BinaryFormat {
  def this(parameters: HeaderParameters) = this(JwtHeader.serializeParams(parameters), parameters)
}

/**
 * @param header the header, containing the JWS/JWE algorithm information.
 * @param claims the base64-decoded "claims" segment (may be encrypted, depending on header information).
 * @param crypto the base64-decoded "crypto" segment.
 */
private[jwt] class JwtImpl(header: JwtHeader, claims: Array[Byte], crypto: Array[Byte]) extends Jwt {
  /**
   * Validates a signature contained in the 'crypto' segment.
   *
   * @param verifier the signature verifier
   */
  def verifySignature(verifier: SignatureVerifier) {
    verifier.verify(signingInput, crypto)
  }

  private[jwt] def signingInput: Array[Byte] = {
    Array.concat(header.bytes, Jwt.PERIOD, Base64.urlEncode(claims))
  }

  /**
   * Allows retrieval of the full token.
   *
   * @return the encoded header, claims and crypto segments concatenated with "." characters
   */
  def bytes: Array[Byte] = {
    Array.concat(Base64.urlEncode(header.bytes), Jwt.PERIOD, Base64.urlEncode(claims), Jwt.PERIOD, Base64.urlEncode(crypto))
  }

  override def toString: String = {
    "TODO"
  }
}


