package za.co.statements.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Persistent representation of a time-limited download token.
 * Replaces the previous in-memory token map so links survive restarts.
 */
@Entity
@Table(name = "download_token")
public class DownloadToken {

    @Id
    @Column(name = "token", nullable = false, updatable = false)
    private String token;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected DownloadToken() {
        // for JPA
    }

    public DownloadToken(final String token, final String path, final Instant expiresAt) {
        this.token = token;
        this.path = path;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public String getPath() {
        return path;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(final Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
