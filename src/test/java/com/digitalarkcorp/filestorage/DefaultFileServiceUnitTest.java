package com.digitalarkcorp.filestorage;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.dto.RenameRequest;
import com.digitalarkcorp.filestorage.api.dto.SortBy;
import com.digitalarkcorp.filestorage.api.dto.SortDir;
import com.digitalarkcorp.filestorage.api.errors.ConflictException;
import com.digitalarkcorp.filestorage.api.errors.ForbiddenException;
import com.digitalarkcorp.filestorage.application.DefaultFileService;
import com.digitalarkcorp.filestorage.application.FileService;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.FileStatus;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DefaultFileServiceUnitTest {

    private FileService service;
    private TestMetadataRepository repo;
    private TestStoragePort storage;

    @BeforeEach
    void setUp() {
        this.repo = new TestMetadataRepository();
        this.storage = new TestStoragePort();
        this.service = new DefaultFileService(repo, storage);
    }

    private static InputStream bytes(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }

    @Test
    void shouldUploadPublicAndListPublicByTag_caseInsensitive() {
        var m1 = service.upload("u1", "a.txt", Visibility.PUBLIC,
                List.of("Demo"), "text/plain", bytes("A"), 1);

        assertNotNull(m1.id());
        assertEquals(Visibility.PUBLIC, m1.visibility());
        assertTrue(
                m1.tags() != null &&
                        m1.tags().stream().anyMatch(t -> t != null && t.equalsIgnoreCase("demo")),
                "tags should contain 'demo' ignoring case"
        );

        var page = service.listPublic(new ListQuery(
                "demo", SortBy.FILENAME, SortDir.ASC, 0, 10
        ));

        assertEquals(1, page.size());
        assertEquals("a.txt", page.get(0).filename());
    }

    @Test
    void renameShouldSucceedForOwnerAndFailForNonOwner() {
        var m = service.upload("u1", "a.txt", Visibility.PUBLIC,
                List.of("x"), "text/plain", bytes("A"), 1);

        // use exatamente o ownerId persistido para evitar qualquer divergência de string
        var owner = m.ownerId();
        var renamed = assertDoesNotThrow(() ->
                service.rename(owner, m.id(), new RenameRequest("a-renamed.txt")));
        assertEquals("a-renamed.txt", renamed.filename());

        // não-dono deve falhar com Forbidden
        assertThrows(ForbiddenException.class,
                () -> service.rename("uX", m.id(), new RenameRequest("should-fail.txt")));
    }

    @Test
    void duplicateFilenameSameOwnerShould409() {
        service.upload("u1", "dup.txt", Visibility.PRIVATE,
                List.of("x"), "text/plain", bytes("A"), 1);

        assertThrows(ConflictException.class, () ->
                service.upload("u1", "dup.txt", Visibility.PRIVATE,
                        List.of("x"), "text/plain", bytes("B"), 1));
    }

    @Test
    void sameContentSameOwnerDifferentNameShould409() {
        service.upload("u1", "n1.txt", Visibility.PRIVATE,
                List.of("x"), "text/plain", bytes("CONTENT"), 7);

        assertThrows(ConflictException.class, () ->
                service.upload("u1", "n2.txt", Visibility.PRIVATE,
                        List.of("x"), "text/plain", bytes("CONTENT"), 7));
    }

    @Test
    void sameContentDifferentOwnerShouldBeAllowed() {
        var a = service.upload("u1", "c.txt", Visibility.PRIVATE,
                List.of("x"), "text/plain", bytes("CONTENT"), 7);
        var b = service.upload("u2", "c.txt", Visibility.PRIVATE,
                List.of("x"), "text/plain", bytes("CONTENT"), 7);

        assertNotEquals(a.ownerId(), b.ownerId());
        assertEquals(a.contentHash(), b.contentHash());
    }

    // ---- Test doubles ----

    static class TestMetadataRepository implements MetadataRepository {
        private final Map<String, FileMetadata> store = new ConcurrentHashMap<>();

        @Override
        public FileMetadata save(FileMetadata m) {
            // simula índices únicos do Mongo, ignorando o próprio id
            boolean ownerFilenameClash = store.values().stream().anyMatch(x ->
                    !Objects.equals(x.id(), m.id()) &&
                            x.ownerId().equals(m.ownerId()) &&
                            x.filename().equals(m.filename()));

            if (ownerFilenameClash) {
                throw new DuplicateKeyException("ux_owner_filename clash");
            }

            boolean ownerHashClash = (m.contentHash() != null) && store.values().stream().anyMatch(x ->
                    !Objects.equals(x.id(), m.id()) &&
                            x.ownerId().equals(m.ownerId()) &&
                            Objects.equals(x.contentHash(), m.contentHash()));

            if (ownerHashClash) {
                throw new DuplicateKeyException("ux_owner_hash clash");
            }

            String id = (m.id() == null) ? UUID.randomUUID().toString().replace("-", "").substring(0, 24) : m.id();
            var now = Instant.now();
            var toSave = new FileMetadata(
                    id,
                    m.ownerId(),
                    m.filename(),
                    m.visibility(),
                    m.tags(),
                    m.size(),
                    m.contentType(),
                    m.contentHash(),
                    m.linkId(),
                    (m.status() == null) ? FileStatus.READY : m.status(),
                    (m.createdAt() == null) ? now : m.createdAt(),
                    now
            );
            store.put(id, toSave);
            return toSave;
        }

        @Override
        public Optional<FileMetadata> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<FileMetadata> findByOwnerAndFilename(String ownerId, String filename) {
            return store.values().stream()
                    .filter(x -> x.ownerId().equals(ownerId) && x.filename().equals(filename))
                    .findFirst();
        }

        @Override
        public Optional<FileMetadata> findByOwnerAndContentHash(String ownerId, String hash) {
            return store.values().stream()
                    .filter(x -> x.ownerId().equals(ownerId) && Objects.equals(x.contentHash(), hash))
                    .findFirst();
        }

        @Override
        public Optional<FileMetadata> findByLinkId(String linkId) {
            return store.values().stream()
                    .filter(x -> Objects.equals(x.linkId(), linkId))
                    .findFirst();
        }

        @Override
        public void deleteById(String id) {
            store.remove(id);
        }

        @Override
        public List<FileMetadata> listPublic(String tag, SortBy sortBy, SortDir sortDir, int page, int size) {
            Comparator<FileMetadata> cmp = Comparator.comparing(FileMetadata::filename);
            if (sortBy == SortBy.UPLOAD_DATE) cmp = Comparator.comparing(FileMetadata::createdAt);
            if (sortDir == SortDir.DESC) cmp = cmp.reversed();

            return store.values().stream()
                    .filter(x -> x.visibility() == Visibility.PUBLIC && x.status() == FileStatus.READY)
                    .filter(x -> tag == null || (x.tags() != null &&
                            x.tags().stream().anyMatch(t -> t != null && t.equalsIgnoreCase(tag))))
                    .sorted(cmp)
                    .skip((long) page * size)
                    .limit(size)
                    .collect(Collectors.toList());
        }

        @Override
        public List<FileMetadata> listByOwner(String ownerId, String tag, SortBy sortBy, SortDir sortDir, int page, int size) {
            Comparator<FileMetadata> cmp = Comparator.comparing(FileMetadata::filename);
            if (sortBy == SortBy.UPLOAD_DATE) cmp = Comparator.comparing(FileMetadata::createdAt);
            if (sortDir == SortDir.DESC) cmp = cmp.reversed();

            return store.values().stream()
                    .filter(x -> x.ownerId().equals(ownerId))
                    .filter(x -> tag == null || (x.tags() != null &&
                            x.tags().stream().anyMatch(t -> t != null && t.equalsIgnoreCase(tag))))
                    .sorted(cmp)
                    .skip((long) page * size)
                    .limit(size)
                    .collect(Collectors.toList());
        }
    }

    static class TestStoragePort implements StoragePort {
        private final Set<String> objects = Collections.synchronizedSet(new HashSet<>());

        @Override
        public String initiate(String objectKey) {
            objects.add(objectKey);
            return "upload-" + objectKey;
        }

        @Override
        public void uploadPart(String uploadId, int partNumber, InputStream data, long size) {
            try { data.readAllBytes(); } catch (Exception ignore) {}
        }

        @Override
        public void complete(String uploadId) {
        }

        @Override
        public InputStream get(String objectKey) {
            if (!objects.contains(objectKey)) throw new NoSuchElementException("not found");
            return new ByteArrayInputStream(new byte[]{});
        }

        @Override
        public void delete(String objectKey) {
            objects.remove(objectKey);
        }
    }
}
