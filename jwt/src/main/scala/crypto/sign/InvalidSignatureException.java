package crypto.sign;

/**
 * @author Luke Taylor
 */
public class InvalidSignatureException extends RuntimeException {
    public InvalidSignatureException(String message) {
        super(message);
    }
}
