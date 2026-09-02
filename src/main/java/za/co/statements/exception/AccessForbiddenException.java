package za.co.statements.exception;

/**
 * Raised when an authenticated user attempts to access statements that do not
 * belong to their own customer account.
 */
public class AccessForbiddenException extends RuntimeException {

    public AccessForbiddenException(final String message) {
        super(message);
    }
}
