package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.api.dto.FileResponse;
import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.api.dto.SortBy;
import com.digitalarkcorp.filestorage.api.dto.SortDir;
import com.digitalarkcorp.filestorage.api.dto.UploadRequest;
import com.digitalarkcorp.filestorage.api.errors.BadRequestException;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.infrastructure.config.PaginationProperties;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class FileController {

    private final FileService service;
    private final PaginationProperties pagination;

    public FileController(FileService service, PaginationProperties pagination) {
        this.service = service;
        this.pagination = pagination;
    }

    @PostMapping(
            path = "/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FileResponse upload(
            @RequestHeader("X-User-Id") String ownerId,
            @RequestPart("metadata") @Valid UploadRequest meta,
            @RequestPart("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File part is required");
        }
        String contentType = (file.getContentType() != null && !file.getContentType().isBlank())
                ? file.getContentType()
                : "application/octet-stream";
        try {
            return FileResponse.from(
                    service.upload(
                            ownerId,
                            meta.filename(),
                            meta.visibility(),
                            meta.tags(),
                            contentType,
                            file.getInputStream(),
                            file.getSize()
                    )
            );
        } catch (Exception e) {
            // converte IOException/Runtime para 500 genérico; handlers específicos tratam casos conhecidos
            throw new RuntimeException("Failed to process upload", e);
        }
    }

    @GetMapping(path = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FileResponse> listMy(
            @RequestHeader("X-User-Id") String ownerId,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "sortBy", required = false) SortBy sortBy,
            @RequestParam(value = "sortDir", required = false) SortDir sortDir,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        SortBy sb = (sortBy != null) ? sortBy : SortBy.UPLOAD_DATE;
        SortDir sd = (sortDir != null) ? sortDir : SortDir.DESC;
        int p = (page != null && page >= 0) ? page : 0;
        int s = (size != null && size > 0) ? Math.min(size, pagination.maxSize()) : pagination.defaultSize();

        return service.listByOwner(ownerId, new ListQuery(tag, sb, sd, p, s))
                .stream()
                .map(FileResponse::from)
                .toList();
    }

    @GetMapping(path = "/files/public", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FileResponse> listPublic(
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "sortBy", required = false) SortBy sortBy,
            @RequestParam(value = "sortDir", required = false) SortDir sortDir,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        SortBy sb = (sortBy != null) ? sortBy : SortBy.UPLOAD_DATE;
        SortDir sd = (sortDir != null) ? sortDir : SortDir.DESC;
        int p = (page != null && page >= 0) ? page : 0;
        int s = (size != null && size > 0) ? Math.min(size, pagination.maxSize()) : pagination.defaultSize();

        return service.listPublic(new ListQuery(tag, sb, sd, p, s))
                .stream()
                .map(FileResponse::from)
                .toList();
    }

    @PatchMapping(path = "/files/{id}/name", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FileResponse rename(
            @RequestHeader("X-User-Id") String ownerId,
            @PathVariable("id") String id,
            @RequestBody @Valid RenameRequest req
    ) {
        return FileResponse.from(service.rename(ownerId, id, req));
    }

    @DeleteMapping(path = "/files/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-User-Id") String ownerId,
            @PathVariable("id") String id
    ) {
        service.delete(ownerId, id);
    }
}
