package jwt

/**
 * @author Luke Taylor
 */

trait JweDecryptor {

  def decrypt(jwt: Jwt)

}
