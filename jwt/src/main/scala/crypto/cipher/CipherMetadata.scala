package crypto.cipher

import crypto.AlgorithmMetadata

/**
 * @author Luke Taylor
 */

trait CipherMetadata extends AlgorithmMetadata {
  /**
   * @return Size of the key in bits.
   */
  def keySize: Int
}
