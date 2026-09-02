package za.co.statements.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of the OIDC token endpoint response relayed back to the browser.
 */
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType
) {
}
