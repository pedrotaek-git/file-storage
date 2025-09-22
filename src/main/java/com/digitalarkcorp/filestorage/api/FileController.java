package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.application.FileService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    // POST /files (multipart/stream)  -> upload
    // GET  /files/public              -> list public
    // GET  /files/my                  -> list my (uses X-User-Id)
    // PATCH /files/{id}/name          -> rename (owner only)
    // DELETE /files/{id}              -> delete (owner only)
}
