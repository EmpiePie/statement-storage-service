package za.co.statements.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class DownloadTokenStore {

    private final Map<String, TokenData> tokens = new ConcurrentHashMap<>();

    private static class TokenData {
        String path;
        Instant expiresAt;
    }

    public String generateToken(final String path, final Duration ttl) {
        String token = UUID.randomUUID().toString();

        TokenData data = new TokenData();
        data.path = path;
        data.expiresAt = Instant.now().plus(ttl);
        tokens.put(token, data);

        log.info("Generated token {} for path={}, expiresAt={}", token, path, data.expiresAt);

        return token;
    }

    public String validateToken(final String token) {
        TokenData data = tokens.get(token);

        if (data == null) {
            log.warn("Token {} not found or expired", token);
            return null;
        }

        if (Instant.now().isAfter(data.expiresAt)) {
            log.warn("Token {} expired at {}", token, data.expiresAt);
            tokens.remove(token);
            return null;
        }

        log.info("Token {} validated successfully", token);
        return data.path;
    }

    @Scheduled(fixedRate = 60_000) // every 1 minute
    public void purgeExpired() {
        int before = tokens.size();

        tokens.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().expiresAt.isBefore(Instant.now());
            if (expired) {
                log.info("Purging expired token={}", entry.getKey());
            }
            return expired;
        });

        int after = tokens.size();
        if (before != after) {
            log.info("Purged {} expired tokens", before - after);
        }
    }

    public void forceExpireToken(final String token) {
        TokenData data = tokens.get(token);
        if (data != null) {
            data.expiresAt = Instant.now().minusSeconds(1);
            log.info("Forced expiry of token {}", token);
        }
    }
}

