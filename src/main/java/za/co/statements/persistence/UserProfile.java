package za.co.statements.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Application-side profile that links an authenticated Keycloak user
 * (by username) to the customerId whose statements they may access.
 * Credentials themselves live in Keycloak, not here.
 */
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "keycloak_user_id")
    private String keycloakUserId;

    protected UserProfile() {
        // for JPA
    }

    public UserProfile(final String username, final Long customerId, final String keycloakUserId) {
        this.username = username;
        this.customerId = customerId;
        this.keycloakUserId = keycloakUserId;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }
}
