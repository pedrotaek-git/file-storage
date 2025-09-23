package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;

@RestController
@RequestMapping("/d")
public class DownloadController {

    private final FileService service;

    public DownloadController(FileService service) {
        this.service = service;
    }

    @GetMapping("/{linkId}")
    public void download(@PathVariable String linkId, HttpServletResponse resp) throws Exception {
        StoragePort.Resource res = service.downloadByLink(linkId);
        if (res == null || res.stream() == null) {
            resp.setStatus(404);
            return;
        }
        if (res.contentType() != null) resp.setContentType(res.contentType());
        if (res.size() > 0) resp.setContentLengthLong(res.size());
        try (InputStream in = res.stream(); OutputStream out = resp.getOutputStream()) {
            in.transferTo(out);
            out.flush();
        }
    }
}
