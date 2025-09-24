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
                        long contentLength,
                        InputStream data);

    List<FileMetadata> listByOwner(String ownerId, ListQuery query);

    List<FileMetadata> listPublic(ListQuery query);

    FileMetadata rename(String userId, String id, RenameRequest req);

    boolean delete(String userId, String id);

    StoragePort.Resource getForDownload(String linkId);

    FileMetadata findByLinkId(String linkId);
}
