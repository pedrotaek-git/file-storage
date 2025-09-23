package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.api.dto.UploadMetadata;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping
@Validated
public class FileController {

    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    @PostMapping(path = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FileMetadata upload(
            @RequestHeader("X-User-Id") String userId,
            @RequestPart("metadata") @Valid UploadMetadata meta,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return service.upload(
                userId,
                meta.filename(),
                meta.visibility(),
                meta.tags(),
                file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE,
                file.getSize(),
                file.getInputStream()
        );
    }

    @GetMapping(path = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FileMetadata> listByOwner(
            @RequestHeader("X-User-Id") String userId,
            @Valid ListQuery query
    ) {
        return service.listByOwner(userId, query);
    }

    @GetMapping(path = "/files/public", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FileMetadata> listPublic(@Valid ListQuery query) {
        return service.listPublic(query);
    }

    @PatchMapping(path = "/files/{id}/name", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FileMetadata rename(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("id") String id,
            @RequestBody @Valid RenameRequest req
    ) {
        return service.rename(userId, id, req);
    }

    @DeleteMapping(path = "/files/{id}")
    public void delete(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("id") String id
    ) {
        service.delete(userId, id);
    }
}
