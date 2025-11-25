package za.co.statements.dto.response;

import java.time.Instant;

public record ErrorResponse(
        String message,
        Instant timestamp
) {
    public ErrorResponse(String message) {
        this(message, Instant.now());
    }
}

