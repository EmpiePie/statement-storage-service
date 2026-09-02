package za.co.statements.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import za.co.statements.auth.dto.TokenResponse;

import java.util.List;
import java.util.Map;

/**
 * Thin client over Keycloak's OIDC token endpoint (login) and Admin REST API
 * (user registration). Keeps Keycloak as the identity provider while the app
 * exposes its own login/register pages.
 */
@Component
@Slf4j
public class KeycloakClient {

    private final RestClient restClient = RestClient.create();

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    /**
     * Exchange user credentials for a realm access token (OIDC password grant).
     *
     * @throws BadCredentialsException if Keycloak rejects the credentials
     */
    public TokenResponse login(final String username, final String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", username);
        form.add("password", password);

        try {
            return restClient.post()
                    .uri(authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.warn("Login failed for user '{}': {}", username, e.getStatusCode());
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    /**
     * Create a user in the realm and set their password.
     *
     * @return the new Keycloak user id
     * @throws UserAlreadyExistsException if the username is taken
     */
    public String register(final String username, final String password) {
        String adminToken = obtainAdminToken();

        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        );
        Map<String, Object> user = Map.of(
                "username", username,
                "enabled", true,
                "credentials", List.of(credential)
        );

        try {
            var response = restClient.post()
                    .uri(authServerUrl + "/admin/realms/" + realm + "/users")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user)
                    .retrieve()
                    .toBodilessEntity();

            // Keycloak returns the new user's id in the Location header
            String location = response.getHeaders().getFirst("Location");
            String keycloakUserId = location != null
                    ? location.substring(location.lastIndexOf('/') + 1)
                    : null;
            log.info("Created Keycloak user '{}' (id={})", username, keycloakUserId);
            return keycloakUserId;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatusCode.valueOf(409)) {
                throw new UserAlreadyExistsException("User '" + username + "' already exists");
            }
            log.error("Failed to create Keycloak user '{}': {} {}",
                    username, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Could not create user in identity provider");
        }
    }

    private String obtainAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        TokenResponse token = restClient.post()
                .uri(authServerUrl + "/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (token == null || token.accessToken() == null) {
            throw new IllegalStateException("Could not obtain Keycloak admin token");
        }
        return token.accessToken();
    }

    /** Thrown when login credentials are rejected. */
    public static class BadCredentialsException extends RuntimeException {
        public BadCredentialsException(final String message) {
            super(message);
        }
    }

    /** Thrown when registering a username that already exists. */
    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(final String message) {
            super(message);
        }
    }
}
