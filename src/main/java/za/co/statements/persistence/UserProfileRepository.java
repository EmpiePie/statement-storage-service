package za.co.statements.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    // Keycloak normalises usernames to lower case, so the JWT's
    // preferred_username may differ in case from the value entered at
    // registration. Match case-insensitively to keep them aligned.
    Optional<UserProfile> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByCustomerId(Long customerId);
}
