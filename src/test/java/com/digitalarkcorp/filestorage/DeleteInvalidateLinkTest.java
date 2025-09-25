package com.digitalarkcorp.filestorage;

import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.application.DefaultFileService;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.testdouble.FakeMetadataRepository;
import com.digitalarkcorp.filestorage.testdouble.FakeStoragePort;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeleteInvalidatesLinkTest {

    @Test
    void deleteThenDownloadIsNotFound() {
        var repo = new FakeMetadataRepository();
        var storage = new FakeStoragePort();
        var clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        FileService service = new DefaultFileService(repo, storage, clock);

        FileMetadata m = service.upload("u1", "a.txt", Visibility.PUBLIC, List.of("Demo"),
                "text/plain", 1, new ByteArrayInputStream("X".getBytes(StandardCharsets.UTF_8)));

        assertNotNull(service.getForDownload(m.linkId())); // existe

        boolean deleted = service.delete("u1", m.id());
        assertTrue(deleted);

        assertThrows(RuntimeException.class, () -> service.getForDownload(m.linkId()));
    }
}
