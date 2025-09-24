package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.api.dto.FileResponse;
import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.UploadMetadata;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.infrastructure.config.PaginationProperties;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static com.digitalarkcorp.filestorage.api.dto.FileResponse.from;

@Validated
@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService service;
    private final PaginationProperties pagination;

    public FileController(FileService service, PaginationProperties pagination) {
        this.service = service;
        this.pagination = pagination;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FileResponse upload(
            @RequestHeader("X-User-Id") @NotBlank String userId,
            @RequestPart("metadata") @Valid UploadMetadata meta,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        Visibility visibility = meta.visibility();
        List<String> tags = meta.tags();
        FileMetadata m = service.upload(
                userId,
                meta.filename(),
                visibility,
                tags,
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
        );
        return from(m);
    }

    @GetMapping(value = "/public", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FileResponse> listPublic(@Valid ListQuery query) {
        ListQuery q = normalize(query);
        return service.listPublic(q).stream().map(FileResponse::from).toList();
    }

    @PatchMapping(value = "/{id}/name", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FileResponse rename(
            @RequestHeader("X-User-Id") @NotBlank String userId,
            @PathVariable("id") String id,
            @RequestBody @Valid RenameRequest req
    ) {
        return from(service.rename(userId, id, req));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FileResponse> listByOwner(
            @RequestHeader("X-User-Id") @NotBlank String userId,
            @Valid ListQuery query
    ) {
        ListQuery q = normalize(query);
        return service.listByOwner(userId, q).stream().map(FileResponse::from).toList();
    }

    private ListQuery normalize(ListQuery in) {
        ListQuery.SortBy sortBy = in.sortBy() != null ? in.sortBy() : ListQuery.SortBy.CREATED_AT;
        ListQuery.SortDir sortDir = in.sortDir() != null ? in.sortDir() : ListQuery.SortDir.DESC;
        int size = in.size() != null && in.size() > 0 ? Math.min(in.size(), pagination.maxSize()) : pagination.defaultSize();
        int page = in.page() != null && in.page() >= 0 ? in.page() : 0;
        return new ListQuery(in.tag(), in.q(), sortBy, sortDir, page, size);
    }

    @DeleteMapping("/{id}")
    public java.util.Map<String, Boolean> delete(
            @RequestHeader("X-User-Id") @NotBlank String userId,
            @PathVariable("id") String id
    ) {
        boolean ok = service.delete(userId, id);
        if (!ok) throw new com.digitalarkcorp.filestorage.api.errors.NotFoundException("file not found");
        return java.util.Map.of("deleted", true);
    }
}
