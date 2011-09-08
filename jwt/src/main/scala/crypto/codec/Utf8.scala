package crypto.codec

import java.nio.charset.Charset
import java.nio.{ByteBuffer, CharBuffer}

/**
 * @author Luke Taylor
 */

object Utf8 {
  implicit def stringToUtf8Bytes(s: CharSequence) = encode(s)
  implicit def utf8BytesToString(bytes: Array[Byte]) = decode(bytes)

  private val CHARSET: Charset = Charset.forName("UTF-8")
  /**
   * Get the bytes of the String in UTF-8 encoded form.
   */
  def encode(string: CharSequence): Array[Byte] = {
    val bytes: ByteBuffer = CHARSET.newEncoder.encode(CharBuffer.wrap(string))
    val bytesCopy = new Array[Byte](bytes.limit)
    System.arraycopy(bytes.array, 0, bytesCopy, 0, bytes.limit)
    bytesCopy
  }

  /**
   * Decode the bytes in UTF-8 form into a String.
   */
  def decode(bytes: Array[Byte]): String = decode(ByteBuffer.wrap(bytes))

  def decode(bytes: ByteBuffer): String = CHARSET.newDecoder.decode(bytes).toString
}
