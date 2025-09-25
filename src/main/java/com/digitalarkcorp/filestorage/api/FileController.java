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
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
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

        var visibility = meta.visibility();
        var tags = meta.tags();

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            try (var tis = TikaInputStream.get(file.getInputStream())) {
                contentType = new Tika().detect(tis, file.getOriginalFilename());
            }
        }

        FileMetadata m = service.upload(
                userId,
                meta.filename(),
                visibility,
                tags,
                contentType,
                file.getSize(),
                file.getInputStream() // novo stream para o upload real
        );
        return from(m);
    }

    @GetMapping("/public")
    public List<FileResponse> listPublic(
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "q",   required = false) String q,
            @RequestParam(name = "sortBy",  required = false, defaultValue = "CREATED_AT") ListQuery.SortBy sortBy,
            @RequestParam(name = "sortDir", required = false, defaultValue = "DESC")       ListQuery.SortDir sortDir,
            @RequestParam(name = "page",    required = false, defaultValue = "0")          Integer page,
            @RequestParam(name = "size",    required = false, defaultValue = "20")         Integer size
    ) {
        var query = normalize(new ListQuery(tag, q, sortBy, sortDir, page, size));
        return service.listPublic(query).stream().map(FileResponse::from).toList();
    }

    @PatchMapping(value = "/{id}/name", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FileResponse rename(
            @RequestHeader("X-User-Id") @NotBlank String userId,
            @PathVariable("id") String id,
            @RequestBody @Valid RenameRequest req
    ) {
        return from(service.rename(userId, id, req));
    }

    @GetMapping
    public List<FileResponse> listByOwner(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "q",   required = false) String q,
            @RequestParam(name = "sortBy",  required = false, defaultValue = "CREATED_AT") ListQuery.SortBy sortBy,
            @RequestParam(name = "sortDir", required = false, defaultValue = "DESC")       ListQuery.SortDir sortDir,
            @RequestParam(name = "page",    required = false, defaultValue = "0")          Integer page,
            @RequestParam(name = "size",    required = false, defaultValue = "20")         Integer size
    ) {
        var query = normalize(new ListQuery(tag, q, sortBy, sortDir, page, size));
        return service.listByOwner(userId, query).stream().map(FileResponse::from).toList();
    }

    private static ListQuery normalize(ListQuery q) {
        Integer pObj = q.page();
        Integer sObj = q.size();

        int page = (pObj == null || pObj < 0) ? 0 : pObj;
        int size = (sObj == null || sObj <= 0) ? 20 : Math.min(sObj, 100);

        var sortBy  = (q.sortBy()  != null) ? q.sortBy()  : ListQuery.SortBy.CREATED_AT;
        var sortDir = (q.sortDir() != null) ? q.sortDir() : ListQuery.SortDir.DESC;

        return new ListQuery(q.tag(), q.q(), sortBy, sortDir, page, size);
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
