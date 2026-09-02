package za.co.statements.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.statements.persistence.DownloadToken;
import za.co.statements.persistence.DownloadTokenRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Issues and validates time-limited download tokens.
 * Tokens are persisted in Postgres so they survive service restarts.
 * The public API is unchanged from the previous in-memory implementation.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DownloadTokenStore {

    private final DownloadTokenRepository repository;

    public String generateToken(final String path, final Duration ttl) {
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(ttl);

        repository.save(new DownloadToken(token, path, expiresAt));

        log.info("Generated token {} for path={}, expiresAt={}", token, path, expiresAt);
        return token;
    }

    public String validateToken(final String token) {
        DownloadToken data = repository.findById(token).orElse(null);

        if (data == null) {
            log.warn("Token {} not found or expired", token);
            return null;
        }

        if (Instant.now().isAfter(data.getExpiresAt())) {
            log.warn("Token {} expired at {}", token, data.getExpiresAt());
            repository.deleteById(token);
            return null;
        }

        log.info("Token {} validated successfully", token);
        return data.getPath();
    }

    @Scheduled(fixedRate = 60_000) // every 1 minute
    @Transactional
    public void purgeExpired() {
        long removed = repository.deleteByExpiresAtBefore(Instant.now());
        if (removed > 0) {
            log.info("Purged {} expired tokens", removed);
        }
    }

    public void forceExpireToken(final String token) {
        repository.findById(token).ifPresent(data -> {
            data.setExpiresAt(Instant.now().minusSeconds(1));
            repository.save(data);
            log.info("Forced expiry of token {}", token);
        });
    }
}
