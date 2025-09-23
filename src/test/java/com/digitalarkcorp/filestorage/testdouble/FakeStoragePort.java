package com.digitalarkcorp.filestorage.testdouble;

import com.digitalarkcorp.filestorage.domain.ports.StoragePort;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakeStoragePort implements StoragePort {

    private static final class Obj {
        final byte[] data;
        final String contentType;
        Obj(byte[] data, String contentType) {
            this.data = data;
            this.contentType = contentType;
        }
    }

    private final Map<String, Obj> store = new ConcurrentHashMap<>();

    @Override
    public void put(String objectKey, InputStream data, long contentLength, String contentType) {
        try {
            byte[] bytes = data.readAllBytes();
            store.put(objectKey, new Obj(bytes, contentType));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Resource get(String objectKey) {
        Obj o = store.get(objectKey);
        if (o == null) throw new RuntimeException("not found");
        return new Resource(new ByteArrayInputStream(o.data), o.data.length, o.contentType);
    }

    @Override
    public void delete(String objectKey) {
        store.remove(objectKey);
    }
}
