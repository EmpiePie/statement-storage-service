package za.co.statements.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Registration payload. The chosen {@code customerId} is the account whose
 * statements this user will own.
 */
public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotNull @Positive Long customerId
) {
}
