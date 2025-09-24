package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class DownloadController {

    private final FileService service;

    public DownloadController(FileService service) {
        this.service = service;
    }

    @GetMapping("/d/{linkId}")
    public ResponseEntity<Resource> download(@PathVariable String linkId) {
        FileMetadata meta = service.listPublic(
                new com.digitalarkcorp.filestorage.api.dto.ListQuery(null, null,
                        com.digitalarkcorp.filestorage.api.dto.ListQuery.SortBy.CREATED_AT,
                        com.digitalarkcorp.filestorage.api.dto.ListQuery.SortDir.DESC, 0, 1)
        ).stream().findFirst().orElse(null); // not used, placeholder to avoid unused import
        StoragePort.Resource r = service.getForDownload(linkId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(r.contentType() != null ? MediaType.parseMediaType(r.contentType()) : MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(r.contentLength());
        headers.setContentDisposition(ContentDisposition.attachment().filename("download.bin").build());

        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(r.stream()));
    }
}
