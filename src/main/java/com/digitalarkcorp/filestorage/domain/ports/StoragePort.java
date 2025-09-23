package com.digitalarkcorp.filestorage.domain.ports;

import java.io.InputStream;

public interface StoragePort {
    record Resource(InputStream stream, long size, String contentType) {}

    void put(String objectKey, String contentType, long size, InputStream content);

    Resource get(String objectKey);

    void delete(String objectKey);
}
