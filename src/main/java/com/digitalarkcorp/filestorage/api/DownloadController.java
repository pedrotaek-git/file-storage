package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import org.springframework.core.io.InputStreamResource;
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
    public ResponseEntity<InputStreamResource> download(@PathVariable String linkId) {
        StoragePort.Resource r = service.getForDownload(linkId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("download.bin").build());
        String ct = r.contentType();
        headers.setContentType((ct != null && !ct.isBlank()) ? MediaType.parseMediaType(ct) : MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(r.contentLength());
        headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(r.stream()));
    }
}
