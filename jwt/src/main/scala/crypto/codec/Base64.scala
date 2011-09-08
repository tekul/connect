package crypto.codec

/**
 * @author Luke Taylor
 */

object Base64 {
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

  def urlEncode(value: CharSequence): Array[Byte] = {
    urlEncode(Utf8.encode(value))
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

  def urlDecode(value: CharSequence): Array[Byte] = {
    urlDecode(Utf8.encode(value))
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
