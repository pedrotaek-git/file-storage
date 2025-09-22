package com.digitalarkcorp.filestorage.application;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;

import java.io.InputStream;
import java.util.List;

public interface FileService {

    FileMetadata upload(String ownerId,
                        String filename,
                        Visibility visibility,
                        List<String> tags,
                        String providedContentType,
                        InputStream data,
                        long contentLength);

    FileMetadata rename(String ownerId, String id, String newFilename);

    void delete(String ownerId, String id);

    FileMetadata findById(String id);

    List<FileMetadata> listPublic(ListQuery query);

    List<FileMetadata> listMy(String ownerId, ListQuery query);

    InputStream downloadByLinkId(String linkId);
}
