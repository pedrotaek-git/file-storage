package com.digitalarkcorp.filestorage.domain.ports;

import java.io.InputStream;

public interface StoragePort {

    record Resource(InputStream stream, long contentLength, String contentType) {}

    void put(String objectKey, InputStream data, long contentLength, String contentType);

    Resource get(String objectKey);

    void delete(String objectKey);
}
