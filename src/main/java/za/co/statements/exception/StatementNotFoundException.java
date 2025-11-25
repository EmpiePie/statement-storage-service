package za.co.statements.exception;

public class StatementNotFoundException extends RuntimeException {

    public StatementNotFoundException(String message) {
        super(message);
    }
}
