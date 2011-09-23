package crypto.codec

import java.nio.charset.Charset
import java.nio.{CharBuffer, ByteBuffer}
import scala.Array

/**
 * Functions for Hex, Base64 and Utf8 encoding/decoding
 */
object Codecs {
  /**
   * Base 64
   */
  def b64Encode(bytes: Array[Byte]) = Base64Codec.encode(bytes)

  def b64Decode(bytes: Array[Byte]) = Base64Codec.decode(bytes)

  // URL-safe versions with no padding chars
  def b64UrlEncode(bytes: Array[Byte]): Array[Byte] = Base64.urlEncode(bytes)

  def b64UrlEncode(value: CharSequence): Array[Byte] = b64UrlEncode(utf8Encode(value))

  def b64UrlDecode(bytes: Array[Byte]): Array[Byte] = Base64.urlDecode(bytes)

  def b64UrlDecode(value: CharSequence): Array[Byte] = b64UrlDecode(utf8Encode(value))

  /**
   * UTF-8 encoding/decoding. Using a charset rather than `String.getBytes` is less forgiving
   * and will raise an exception for invalid data.
   */
  private val UTF8 = Charset.forName("UTF-8")

  def utf8Encode(string: CharSequence): Array[Byte] = {
    val bytes = UTF8.newEncoder.encode(CharBuffer.wrap(string))
    val bytesCopy = new Array[Byte](bytes.limit)
    System.arraycopy(bytes.array, 0, bytesCopy, 0, bytes.limit)
    bytesCopy
  }

  def utf8Decode(bytes: Array[Byte]): String = utf8Decode(ByteBuffer.wrap(bytes))

  def utf8Decode(bytes: ByteBuffer): String = UTF8.newDecoder.decode(bytes).toString

  def hexEncode(bytes: Array[Byte]) = Hex.encode(bytes)

  def hexDecode(s: CharSequence) = Hex.decode(s)

}

private object Base64 {
  val EQUALS = '='.asInstanceOf[Byte]

  def encode(bytes: Array[Byte]) = Base64Codec.encode(bytes)

  def decode(bytes: Array[Byte]) = Base64Codec.decode(bytes)

  def urlEncode(bytes: Array[Byte]): Array[Byte] = {
    val b64Bytes = Base64Codec.encodeBytesToBytes(bytes, 0, bytes.length, Base64Codec.URL_SAFE)

    var length: Int = b64Bytes.length;

    while(b64Bytes(length - 1) == EQUALS) {
      length -= 1
    }

    var result = new Array[Byte](length)
    System.arraycopy(b64Bytes, 0, result, 0, length)

    result
  }

  def urlDecode(b64: Array[Byte]): Array[Byte] = {
    // Pad with '=' as necessary before feeding to standard decoder
    val b64Bytes = b64.length % 4 match {
      case 0 => b64
      case 2 => pad(b64, 2)
      case 3 => pad(b64, 1)
      case _ => throw new IllegalArgumentException("Invalid Base64 string")
    }

    Base64Codec.decode(b64Bytes, 0, b64Bytes.length, Base64Codec.URL_SAFE)
  }

  private def pad(bytes: Array[Byte], n: Int) = {
    val l = bytes.length
    val padded = new Array[Byte](l + n)
    System.arraycopy(bytes, 0, padded, 0, l)
    for (i <- l until l+n) {
      padded(i) = EQUALS
    }
    padded
  }
}

private object Hex {
  private val HEX = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

  def encode(bytes: Array[Byte]): Array[Char] = {
    val nBytes = bytes.length
    val result = new Array[Char](2 * nBytes)

    var j = 0
    for (i <- 0 until nBytes) {
      // Char for top 4 bits
      result(j) = HEX((0xF0 & bytes(i)) >>> 4)
      // Bottom 4
      result(j+1) = HEX((0x0F & bytes(i)))
      j += 2
    }
    result
  }

  def decode(s: CharSequence): Array[Byte] = {
    val nChars = s.length
    require(nChars % 2 == 0, "Hex-encoded string must have an even number of characters")

    val result = new Array[Byte](nChars / 2)

    for (i <- 0 until nChars by 2) {
      val msb = Character.digit(s.charAt(i), 16)
      val lsb = Character.digit(s.charAt(i + 1), 16)
      require(msb > 0 && lsb > 0, "Non-hex character in input: " + s)
      result(i / 2) = ((msb << 4) | lsb).asInstanceOf[Byte]
    }
    result
  }
}
