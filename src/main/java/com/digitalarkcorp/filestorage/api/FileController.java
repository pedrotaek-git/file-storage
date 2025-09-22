package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.api.dto.*;
import com.digitalarkcorp.filestorage.api.errors.BadRequestException;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping
public class FileController {

    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    private static String requireUserId(String header) {
        if (header == null || header.isBlank()) {
            throw new BadRequestException("Missing X-User-Id header");
        }
        return header;
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FileResponse upload(
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @RequestPart("metadata") @Valid UploadRequest meta,
            @RequestPart("file") MultipartFile file
    ) throws IOException {

        String ownerId = requireUserId(userId);

        String contentType = (file.getContentType() != null && !file.getContentType().isBlank())
                ? file.getContentType()
                : MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

        // use the enum directly (no valueOf)
        Visibility visibility = meta.visibility();

        FileMetadata saved = service.upload(
                ownerId,
                meta.filename(),
                visibility,
                meta.tags(),
                contentType,
                file.getInputStream(),
                file.getSize()
        );
        return FileResponse.from(saved);
    }

    @GetMapping(value = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FileResponse> listMine(
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @Valid ListQuery query
    ) {
        String ownerId = requireUserId(userId);
        return service.listMine(ownerId, query).stream().map(FileResponse::from).toList();
    }

    @GetMapping(value = "/files/public", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FileResponse> listPublic(@Valid ListQuery query) {
        return service.listPublic(query).stream().map(FileResponse::from).toList();
    }

    @PatchMapping(value = "/files/{id}/name", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FileResponse rename(
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @PathVariable("id") String id,
            @RequestBody @Valid RenameRequest req
    ) {
        String ownerId = requireUserId(userId);
        return FileResponse.from(service.rename(ownerId, id, req.newFilename()));
    }

    @DeleteMapping("/files/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @PathVariable("id") String id
    ) {
        String ownerId = requireUserId(userId);
        service.delete(ownerId, id);
        return ResponseEntity.noContent().build();
    }
}
