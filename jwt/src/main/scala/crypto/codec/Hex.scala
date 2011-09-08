package crypto.codec

/**
 * @author Luke Taylor
 */

object Hex {
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
