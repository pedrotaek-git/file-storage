package com.digitalarkcorp.filestorage;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.api.errors.ConflictException;
import com.digitalarkcorp.filestorage.api.errors.ForbiddenException;
import com.digitalarkcorp.filestorage.application.DefaultFileService;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.testdouble.FakeMetadataRepository;
import com.digitalarkcorp.filestorage.testdouble.FakeStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultFileServiceUnitTest {

    private FileService service;

    @BeforeEach
    void setup() {
        service = new DefaultFileService(new FakeMetadataRepository(), new FakeStoragePort());
    }

    @Test
    void uploadPublic_listByTag() {
        service.upload("u1", "a.txt", Visibility.PUBLIC, List.of("Demo"), "text/plain", 1, bin("A"));
        var page = service.listPublic(new ListQuery("demo", null, ListQuery.SortBy.FILENAME, ListQuery.SortDir.ASC, 0, 10));
        assertEquals(1, page.size());
        assertEquals("a.txt", page.get(0).filename());
    }

    @Test
    void rename_ownerOk_otherForbidden() {
        FileMetadata m = service.upload("u1", "a.txt", Visibility.PUBLIC, List.of("Demo"), "text/plain", 1, bin("A"));
        FileMetadata r = service.rename("u1", m.id(), new RenameRequest("a-renamed.txt"));
        assertEquals("a-renamed.txt", r.filename());
        assertThrows(ForbiddenException.class, () -> service.rename("uX", m.id(), new RenameRequest("x.txt")));
    }

    @Test
    void upload_conflict_sameOwner_sameName() {
        service.upload("u2", "dup.txt", Visibility.PRIVATE, List.of("x"), "text/plain", 1, bin("A"));
        assertThrows(ConflictException.class, () ->
                service.upload("u2", "dup.txt", Visibility.PRIVATE, List.of("x"), "text/plain", 1, bin("B")));
    }

    @Test
    void upload_conflict_sameOwner_sameContent() {
        service.upload("u2", "a.txt", Visibility.PRIVATE, List.of("x"), "text/plain", 7, bin("CONTENT"));
        assertThrows(ConflictException.class, () ->
                service.upload("u2", "b.txt", Visibility.PRIVATE, List.of("x"), "text/plain", 7, bin("CONTENT")));
    }

    @Test
    void upload_sameContent_diffOwner_ok() {
        service.upload("u1", "a.txt", Visibility.PRIVATE, List.of("x"), "text/plain", 7, bin("CONTENT"));
        service.upload("u2", "a.txt", Visibility.PRIVATE, List.of("x"), "text/plain", 7, bin("CONTENT"));
    }

    private static InputStream bin(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }
}
