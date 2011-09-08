package crypto.sign

import crypto.AlgorithmMetadata

trait Signer extends AlgorithmMetadata {

  def sign(bytes: Array[Byte]): Array[Byte]
}

trait SignatureVerifier extends AlgorithmMetadata {

  def verify(content: Array[Byte], signature: Array[Byte])
}

trait SignerVerifier extends Signer with SignatureVerifier
