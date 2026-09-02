package za.co.statements.service;

import java.util.List;

/**
 * Abstraction over the statement blob store.
 * Backed by MinIO/S3 in normal runs ({@link S3StorageService}) and by an
 * in-memory map during tests ({@link InMemoryStorageService}).
 */
public interface StorageService {

    void upload(String path, byte[] content);

    byte[] read(String path);

    boolean exists(String path);

    List<String> list(String prefix);
}
