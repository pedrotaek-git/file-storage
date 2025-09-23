package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.application.FileService;
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
        StoragePort.Resource r = service.getForDownload(linkId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(r.contentType()));
        headers.setContentLength(r.contentLength());
        // No filename() on StoragePort.Resource in your codebase; use a generic name
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("download.bin")
                        .build()
        );

        InputStreamResource body = new InputStreamResource(r.stream());
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
