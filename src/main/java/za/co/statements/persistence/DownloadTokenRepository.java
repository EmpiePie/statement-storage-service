package za.co.statements.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface DownloadTokenRepository extends JpaRepository<DownloadToken, String> {

    /**
     * Bulk-delete every token that expired before the given instant.
     *
     * @return number of tokens removed
     */
    long deleteByExpiresAtBefore(Instant instant);
}
