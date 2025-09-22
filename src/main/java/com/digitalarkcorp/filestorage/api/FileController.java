package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.api.dto.FileResponse;
import com.digitalarkcorp.filestorage.api.dto.UploadRequest;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

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

            FileResponse resp = new FileResponse(
                    saved.id(),
                    saved.ownerId(),
                    saved.filename(),
                    saved.visibility(),
                    saved.tags(),
                    saved.size(),
                    saved.contentType(),
                    saved.linkId(),
                    saved.status(),
                    saved.createdAt(),
                    saved.updatedAt()
            );
            return ResponseEntity.ok(resp);
        }
    }
}
