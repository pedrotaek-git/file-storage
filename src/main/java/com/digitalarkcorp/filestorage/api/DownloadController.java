package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.application.FileService;
import org.springframework.http.HttpHeaders;
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

    @GetMapping(path = "/d/{linkId}")
    public ResponseEntity<byte[]> downloadByLink(@PathVariable("linkId") String linkId) {
        try (InputStream in = service.downloadByLink(linkId)) {
            byte[] bytes = in.readAllBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download", e);
        }
    }
}
