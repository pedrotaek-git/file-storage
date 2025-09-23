package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.infrastructure.config.PaginationProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService service;
    private final PaginationProperties pagination;

    public FileController(FileService service, PaginationProperties pagination) {
        this.service = service;
        this.pagination = pagination;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileMetadata upload(@RequestHeader("X-User-Id") String ownerId,
                               @RequestPart("metadata") UploadMetadata meta,
                               @RequestPart("file") MultipartFile file) throws Exception {
        return service.upload(
                ownerId,
                meta.filename(),
                meta.visibility(),
                meta.tags(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
        );
    }

    @GetMapping
    public List<FileMetadata> listByOwner(@RequestHeader("X-User-Id") String ownerId,
                                          @RequestParam(value = "tag", required = false) String tag,
                                          @RequestParam(value = "q", required = false) String filenameContains,
                                          @RequestParam(value = "sortBy", defaultValue = "CREATED_AT") ListQuery.SortBy sortBy,
                                          @RequestParam(value = "sortDir", defaultValue = "DESC") ListQuery.SortDir sortDir,
                                          @RequestParam(value = "page", defaultValue = "0") int page,
                                          @RequestParam(value = "size", required = false) Integer sizeOpt) {
        int size = sizeOpt == null ? pagination.defaultSize() : Math.min(sizeOpt, pagination.maxSize());
        ListQuery q = new ListQuery(tag, filenameContains, sortBy, sortDir, page, size);
        return service.listByOwner(ownerId, q);
    }

    @GetMapping("/public")
    public List<FileMetadata> listPublic(@RequestParam(value = "tag", required = false) String tag,
                                         @RequestParam(value = "q", required = false) String filenameContains,
                                         @RequestParam(value = "sortBy", defaultValue = "CREATED_AT") ListQuery.SortBy sortBy,
                                         @RequestParam(value = "sortDir", defaultValue = "DESC") ListQuery.SortDir sortDir,
                                         @RequestParam(value = "page", defaultValue = "0") int page,
                                         @RequestParam(value = "size", required = false) Integer sizeOpt) {
        int size = sizeOpt == null ? pagination.defaultSize() : Math.min(sizeOpt, pagination.maxSize());
        ListQuery q = new ListQuery(tag, filenameContains, sortBy, sortDir, page, size);
        return service.listPublic(q);
    }

    @PatchMapping("/{id}/name")
    public FileMetadata rename(@RequestHeader("X-User-Id") String ownerId,
                               @PathVariable String id,
                               @RequestBody RenameRequest req) {
        return service.rename(ownerId, id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestHeader("X-User-Id") String ownerId,
                       @PathVariable String id) {
        service.delete(ownerId, id);
    }

    public record UploadMetadata(String filename, Visibility visibility, List<String> tags) {}
}
