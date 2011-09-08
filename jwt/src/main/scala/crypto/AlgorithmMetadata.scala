package crypto

/**
 * @author Luke Taylor
 */

trait AlgorithmMetadata {
  /**
   * @return the JCA/JCE algorithm name.
   */
  def algorithm: String
}
