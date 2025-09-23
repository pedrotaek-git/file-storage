package com.digitalarkcorp.filestorage;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.application.DefaultFileService;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.testdouble.FakeMetadataRepository;
import com.digitalarkcorp.filestorage.testdouble.FakeStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultFileServiceUnitTest {

    private FakeMetadataRepository repo;
    private FakeStoragePort storage;
    private FileService service;

    @BeforeEach
    void setup() {
        repo = new FakeMetadataRepository();
        storage = new FakeStoragePort();
        service = new DefaultFileService(repo, storage);
    }

    @Test
    void uploadPublic_listByTag() {
        FileMetadata m = service.upload(
                "u1", "a.txt", Visibility.PUBLIC, List.of("Demo"),
                "text/plain", len("A"), in("A")
        );
        assertNotNull(m.id());
        List<FileMetadata> pub = service.listPublic(
                new ListQuery("demo", null, ListQuery.SortBy.FILENAME, ListQuery.SortDir.ASC, 0, 10)
        );
        assertEquals(1, pub.size());
        assertEquals("a.txt", pub.get(0).filename());
    }

    @Test
    void rename_ownerOk_otherForbidden() {
        FileMetadata m = service.upload(
                "u1", "a.txt", Visibility.PUBLIC, List.of("Demo"),
                "text/plain", len("A"), in("A")
        );
        FileMetadata r = service.rename("u1", m.id(), new RenameRequest("a-renamed.txt"));
        assertEquals("a-renamed.txt", r.filename());
        assertThrows(RuntimeException.class, () ->
                service.rename("uX", m.id(), new RenameRequest("should-fail.txt"))
        );
    }

    @Test
    void upload_conflict_sameOwner_sameName() {
        service.upload("u1", "dup.txt", Visibility.PRIVATE, List.of("x"),
                "text/plain", len("A"), in("A"));
        assertThrows(RuntimeException.class, () ->
                service.upload("u1", "dup.txt", Visibility.PRIVATE, List.of("x"),
                        "text/plain", len("B"), in("B"))
        );
    }

    @Test
    void upload_conflict_sameOwner_sameContent() {
        service.upload("u1", "x1.txt", Visibility.PRIVATE, List.of("x"),
                "text/plain", len("CONTENT"), in("CONTENT"));
        assertThrows(RuntimeException.class, () ->
                service.upload("u1", "x2.txt", Visibility.PRIVATE, List.of("x"),
                        "text/plain", len("CONTENT"), in("CONTENT"))
        );
    }

    @Test
    void upload_sameContent_diffOwner_ok() {
        service.upload("u1", "same.txt", Visibility.PRIVATE, List.of("x"),
                "text/plain", len("SAME"), in("SAME"));
        FileMetadata m2 = service.upload("other", "same.txt", Visibility.PRIVATE, null,
                "text/plain", len("SAME"), in("SAME"));
        assertEquals("other", m2.ownerId());
    }

    private static ByteArrayInputStream in(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    private static long len(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
    }
}
