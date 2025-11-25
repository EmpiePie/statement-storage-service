package za.co.statements.exception;

public class StatementNotFoundException extends RuntimeException {

    public StatementNotFoundException(final String message) {
        super(message);
    }
}
