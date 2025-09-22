package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.api.dto.*;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> upload(
            @RequestHeader("X-User-Id") String ownerId,
            @RequestPart("metadata") @Valid UploadRequest metadata,
            @RequestPart("file") MultipartFile file
    ) throws Exception {

        if (!StringUtils.hasText(ownerId)) {
            return ResponseEntity.badRequest().build();
        }

        final String filename = metadata.filename();
        final Visibility visibility = metadata.visibility();
        final var tags = metadata.tags();
        final String providedContentType = (metadata.contentType() != null && !metadata.contentType().isBlank())
                ? metadata.contentType()
                : file.getContentType();

        try (InputStream in = file.getInputStream()) {
            FileMetadata saved = service.upload(
                    ownerId,
                    filename,
                    visibility,
                    tags,
                    providedContentType,
                    in,
                    file.getSize()
            );

            return ResponseEntity.ok(toResponse(saved));
        }
    }

    @GetMapping
    public ResponseEntity<List<FileResponse>> listMine(
            @RequestHeader("X-User-Id") String ownerId,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sortBy", required = false) SortBy sortBy,
            @RequestParam(value = "sortDir", required = false) SortDir sortDir
    ) {
        if (!StringUtils.hasText(ownerId)) {
            return ResponseEntity.badRequest().build();
        }
        ListQuery q = new ListQuery(tag, sortBy, sortDir, page, size);
        List<FileMetadata> list = service.listMy(ownerId, q);
        return ResponseEntity.ok(list.stream().map(this::toResponse).toList());
    }

    @GetMapping("/public")
    public ResponseEntity<List<FileResponse>> listPublic(
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sortBy", required = false) SortBy sortBy,
            @RequestParam(value = "sortDir", required = false) SortDir sortDir
    ) {
        ListQuery q = new ListQuery(tag, sortBy, sortDir, page, size);
        List<FileMetadata> list = service.listPublic(q);
        return ResponseEntity.ok(list.stream().map(this::toResponse).toList());
    }

    @PatchMapping(path = "/{id}/name", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileResponse> rename(
            @RequestHeader("X-User-Id") String ownerId,
            @PathVariable("id") String id,
            @RequestBody @Valid RenameRequest body
    ) {
        if (!StringUtils.hasText(ownerId)) {
            return ResponseEntity.badRequest().build();
        }
        FileMetadata updated = service.rename(ownerId, id, body.newFilename());
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") String ownerId,
            @PathVariable("id") String id
    ) {
        if (!StringUtils.hasText(ownerId)) {
            return ResponseEntity.badRequest().build();
        }
        service.delete(ownerId, id);
        return ResponseEntity.noContent().build();
    }

    private FileResponse toResponse(FileMetadata m) {
        return new FileResponse(
                m.id(),
                m.ownerId(),
                m.filename(),
                m.visibility(),
                m.tags(),
                m.size(),
                m.contentType(),
                m.linkId(),
                m.status(),
                m.createdAt(),
                m.updatedAt()
        );
    }
}
