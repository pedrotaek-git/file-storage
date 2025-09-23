package com.digitalarkcorp.filestorage.application;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface FileService {

    FileMetadata upload(String ownerId,
                        String filename,
                        Visibility visibility,
                        List<String> tags,
                        String contentType,
                        InputStream data,
                        long size);

    FileMetadata rename(String ownerId,
                        String fileId,
                        RenameRequest request);

    void delete(String ownerId, String fileId);

    InputStream downloadByLink(String linkId);

    Optional<FileMetadata> findById(String id);

    List<FileMetadata> listPublic(ListQuery query);

    List<FileMetadata> listByOwner(String ownerId, ListQuery query);
}
