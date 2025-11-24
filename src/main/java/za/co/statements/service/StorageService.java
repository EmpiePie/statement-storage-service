package za.co.statements.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StorageService {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    public void upload(String path, byte[] content) {
        store.put(path, content);
    }

    public byte[] read(String path) {
        return store.get(path);
    }

    public boolean exists(String path) {
        return store.containsKey(path);
    }

    public List<String> list(String prefix) {
        return store.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .toList();
    }
}


