package za.co.statements.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.statements.auth.dto.LoginRequest;
import za.co.statements.auth.dto.RegisterRequest;
import za.co.statements.auth.dto.TokenResponse;
import za.co.statements.dto.response.ErrorResponse;
import za.co.statements.persistence.UserProfile;
import za.co.statements.persistence.UserProfileRepository;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Register and log in via the Keycloak identity provider")
public class AuthController {

    private final KeycloakClient keycloakClient;
    private final UserProfileRepository userProfileRepository;

    @Operation(summary = "Register a new account (creates a Keycloak user + local profile)")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody final RegisterRequest request) {
        if (userProfileRepository.existsByUsernameIgnoreCase(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Username already registered"));
        }
        if (userProfileRepository.existsByCustomerId(request.customerId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Customer ID already claimed by another account"));
        }

        String keycloakUserId = keycloakClient.register(request.username(), request.password());
        userProfileRepository.save(
                new UserProfile(request.username(), request.customerId(), keycloakUserId));

        log.info("Registered user '{}' for customerId={}", request.username(), request.customerId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ErrorResponse("Account created. You can now log in."));
    }

    @Operation(summary = "Log in and receive a JWT access token")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody final LoginRequest request) {
        TokenResponse token = keycloakClient.login(request.username(), request.password());
        return ResponseEntity.ok(token);
    }
}
