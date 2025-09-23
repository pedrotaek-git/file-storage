package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/d")
public class DownloadController {

    private final FileService service;

    public DownloadController(FileService service) {
        this.service = service;
    }

    @GetMapping("/{linkId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String linkId) {
        StoragePort.Resource r = service.downloadByLink(linkId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, r.contentType() == null ? "application/octet-stream" : r.contentType())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(Math.max(r.length(), 0)))
                .body(new InputStreamResource(r.stream()));
    }
}
