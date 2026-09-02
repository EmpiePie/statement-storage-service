package za.co.statements.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory storage used for tests (active under the {@code test} profile),
 * so the test suite does not require a running MinIO/S3 container.
 */
@Service
@Profile("test")
public class InMemoryStorageService implements StorageService {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public void upload(final String path, final byte[] content) {
        store.put(path, content);
    }

    @Override
    public byte[] read(final String path) {
        return store.get(path);
    }

    @Override
    public boolean exists(final String path) {
        return store.containsKey(path);
    }

    @Override
    public List<String> list(final String prefix) {
        return store.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .toList();
    }
}
