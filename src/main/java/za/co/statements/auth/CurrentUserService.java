package za.co.statements.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import za.co.statements.exception.AccessForbiddenException;
import za.co.statements.persistence.UserProfile;
import za.co.statements.persistence.UserProfileRepository;

/**
 * Resolves the authenticated caller and enforces that they may only operate on
 * statements belonging to their own customer account.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {

    private final UserProfileRepository userProfileRepository;

    /**
     * Ensure the current user owns {@code customerId}. No-op when the request is
     * unauthenticated (e.g. the permit-all test profile) so tests need no JWT.
     *
     * @throws AccessForbiddenException if the user owns a different customer id
     */
    public void assertOwnership(final Long customerId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return; // not a JWT-authenticated request (tests / non-API paths)
        }

        String username = jwt.getClaimAsString("preferred_username");
        UserProfile profile = userProfileRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new AccessForbiddenException(
                        "No profile is linked to user '" + username + "'"));

        if (!profile.getCustomerId().equals(customerId)) {
            log.warn("User '{}' (customerId={}) attempted to access customerId={}",
                    username, profile.getCustomerId(), customerId);
            throw new AccessForbiddenException(
                    "You are not allowed to access statements for customer " + customerId);
        }
    }
}
