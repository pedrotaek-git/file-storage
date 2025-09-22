package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.application.FileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

@RestController
public class DownloadController {

    private final FileService service;

    public DownloadController(FileService service) {
        this.service = service;
    }

    @GetMapping("/d/{linkId}")
    public ResponseEntity<byte[]> download(@PathVariable("linkId") String linkId) throws Exception {
        try (InputStream in = service.downloadByLinkId(linkId)) {
            byte[] bytes = in.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        }
    }
}
