package com.digitalarkcorp.filestorage.application;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;

import java.io.InputStream;
import java.util.List;

public interface FileService {
    FileMetadata upload(String ownerId,
                        String filename,
                        Visibility visibility,
                        List<String> tags,
                        String contentType,
                        long size,
                        InputStream in);

    List<FileMetadata> listByOwner(String ownerId, ListQuery query);

    List<FileMetadata> listPublic(ListQuery query);

    FileMetadata rename(String ownerId, String id, RenameRequest req);

    void delete(String ownerId, String id);

    StoragePort.Resource downloadByLink(String linkId);
}
