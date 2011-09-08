package jwt

import crypto.cipher.CipherMetadata

/**
 * Helper for converting JWE/JWS names to and from JCA/JCE names
 *
 * @author Luke Taylor
 */

private[jwt] object JwtAlgorithms {
  private val sigAlgs = Map(
    "HS256" -> "HMACSHA256",
    "HS384" -> "HMACSHA384",
    "HS512" -> "HMACSHA512",
    "RS256" -> "SHA256withRSA",
    "RS512" -> "SHA512withRSA"
  )
  private val javaToSigAlgs = sigAlgs map {_.swap}

  private val keyAlgs = Map(
    "RSA1_5" -> "RSA/ECB/PKCS1Padding"
  )
  private val javaToKeyAlgs = keyAlgs map {_.swap}

  def sigAlg(javaName: String): String = javaToSigAlgs.get(javaName) match {
    case(Some(alg)) => alg
    case None => throw new IllegalArgumentException("Invalid or unsupported signature algorithm: " + javaName)
  }

  def keyEncryptionAlg(javaName: String) = javaToKeyAlgs.get(javaName) match {
    case(Some(alg)) => alg
    case None => throw new IllegalArgumentException("Invalid or unsupported key encryption algorithm: " + javaName)
  }

  def enc(cipher: CipherMetadata): String = {
    require(cipher.algorithm.equalsIgnoreCase("AES/CBC/PKCS5Padding"), "Unknown or unsupported algorithm")
    cipher.keySize match {
      case 128 => "A128CBC"
      case 256 => "A256CBC"
      case _ => throw new IllegalArgumentException("Unsupported key size")
    }
  }

}




