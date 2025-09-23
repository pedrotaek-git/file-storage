package com.digitalarkcorp.filestorage.testdouble;

import com.digitalarkcorp.filestorage.domain.ports.StoragePort;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakeStoragePort implements StoragePort {
    private final Map<String, byte[]> data = new ConcurrentHashMap<>();
    private final Map<String, String> types = new ConcurrentHashMap<>();

    @Override
    public void put(String objectKey, String contentType, long size, InputStream content) {
        try {
            byte[] bytes = content.readAllBytes();
            data.put(objectKey, bytes);
            types.put(objectKey, contentType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Resource get(String objectKey) {
        byte[] bytes = data.get(objectKey);
        if (bytes == null) return null;
        return new Resource(new ByteArrayInputStream(bytes), bytes.length, types.get(objectKey));
    }

    @Override
    public void delete(String objectKey) {
        data.remove(objectKey);
        types.remove(objectKey);
    }
}
