package com.digitalarkcorp.filestorage.application;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;

import java.io.InputStream;
import java.util.List;

public interface FileService {
    FileMetadata upload(String ownerId, String filename, Visibility visibility, List<String> tags,
                        String contentType, InputStream data, long size);

    List<FileMetadata> listPublic(ListQuery query);

    List<FileMetadata> listMine(String ownerId, ListQuery query);

    FileMetadata rename(String ownerId, String id, String newFilename);

    void delete(String ownerId, String id);

    InputStream downloadByLinkId(String linkId);

    FileMetadata findById(String id);
}
