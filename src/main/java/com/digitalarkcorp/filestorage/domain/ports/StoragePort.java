package com.digitalarkcorp.filestorage.domain.ports;

import java.io.InputStream;

public interface StoragePort {
    String initiate(String objectKey);
    void uploadPart(String uploadId, int partNumber, InputStream data, long size);
    void complete(String uploadId);
    InputStream get(String objectKey);
    void delete(String objectKey);
}
