package com.digitalarkcorp.filestorage.api;

import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DownloadControllerHeadersTest {

    @Test
    void download_sets_headers_disposition_ranges_type_length_and_etag() {
        FileService service = Mockito.mock(FileService.class);
        DownloadController controller = new DownloadController(service);

        String linkId = "test-link";
        byte[] data = "hello".getBytes();

        // Resource: (InputStream, long contentLength, String contentType)
        StoragePort.Resource resource =
                new StoragePort.Resource(new ByteArrayInputStream(data), data.length, "text/plain");
        when(service.getForDownload(linkId)).thenReturn(resource);

        // Metadata com enum aninhado FileMetadata.FileStatus
        FileMetadata meta = new FileMetadata(
                "id1", "u1", "x.txt", Visibility.PUBLIC, List.of("Demo"),
                data.length, "text/plain",
                "abc123hash",  // será usado como ETag
                linkId, FileMetadata.FileStatus.READY,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z")
        );
        when(service.findByLinkId(linkId)).thenReturn(meta);

        ResponseEntity<InputStreamResource> resp = controller.download(linkId);

        assertEquals(200, resp.getStatusCode().value());
        var h = resp.getHeaders();

        // Content-Disposition
        assertEquals("attachment; filename=\"download.bin\"", h.getFirst("Content-Disposition"));
        // Accept-Ranges
        assertEquals("bytes", h.getFirst("Accept-Ranges"));
        // Content-Type
        assertEquals("text/plain", h.getFirst("Content-Type"));
        // Content-Length
        assertEquals(String.valueOf(data.length), h.getFirst("Content-Length"));
        // ETag
        assertEquals("\"abc123hash\"", h.getETag());

        assertNotNull(resp.getBody());
    }
}
