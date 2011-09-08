package jwt

/**
 * @author Luke Taylor
 */

trait BinaryFormat {
  def bytes: Array[Byte]
}
