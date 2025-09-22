package com.digitalarkcorp.filestorage.application;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;

import java.io.InputStream;
import java.util.List;

public interface FileService {

    /**
     * Stores a file and persists its metadata.
     */
    FileMetadata upload(String ownerId,
                        String filename,
                        Visibility visibility,
                        List<String> tags,
                        String contentType,
                        InputStream content,
                        long size);

    /**
     * Lists files owned by a specific user using filtering/sorting/pagination.
     */
    List<FileMetadata> listByOwner(String ownerId, ListQuery query);

    /**
     * Lists public files using filtering/sorting/pagination.
     */
    List<FileMetadata> listPublic(ListQuery query);

    /**
     * Renames a file if requester is the owner.
     */
    FileMetadata rename(String ownerId, String id, RenameRequest request);

    /**
     * Downloads a file by its public link identifier.
     */
    InputStream downloadByLink(String linkId);

    /**
     * Deletes a file if requester is the owner.
     */
    void delete(String ownerId, String id);
}
