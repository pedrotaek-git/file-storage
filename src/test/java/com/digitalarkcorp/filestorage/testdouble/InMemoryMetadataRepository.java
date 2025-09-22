package com.digitalarkcorp.filestorage.testdouble;

import com.digitalarkcorp.filestorage.api.dto.SortBy;
import com.digitalarkcorp.filestorage.api.dto.SortDir;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.FileStatus;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryMetadataRepository implements MetadataRepository {

    private final Map<String, FileMetadata> byId = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    @Override
    public FileMetadata save(FileMetadata m) {
        String id = (m.id() == null || m.id().isBlank()) ? newId() : m.id();
        Instant created = (m.createdAt() == null) ? Instant.now() : m.createdAt();
        Instant updated = (m.updatedAt() == null) ? Instant.now() : m.updatedAt();

        FileMetadata stored = new FileMetadata(
                id,
                m.ownerId(),
                m.filename(),
                m.visibility(),
                m.tags(),
                m.size(),
                m.contentType(),
                m.contentHash(),
                m.linkId(),
                m.status() == null ? FileStatus.READY : m.status(),
                created,
                updated
        );
        byId.put(id, stored);
        return stored;
    }

    @Override
    public Optional<FileMetadata> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<FileMetadata> findByOwnerAndFilename(String ownerId, String filename) {
        return byId.values().stream()
                .filter(f -> Objects.equals(f.ownerId(), ownerId) && Objects.equals(f.filename(), filename))
                .findFirst();
    }

    @Override
    public Optional<FileMetadata> findByOwnerAndContentHash(String ownerId, String contentHash) {
        return byId.values().stream()
                .filter(f -> Objects.equals(f.ownerId(), ownerId) && Objects.equals(f.contentHash(), contentHash))
                .findFirst();
    }

    @Override
    public Optional<FileMetadata> findByLinkId(String linkId) {
        return byId.values().stream()
                .filter(f -> Objects.equals(f.linkId(), linkId))
                .findFirst();
    }

    @Override
    public void deleteById(String id) {
        byId.remove(id);
    }

    @Override
    public List<FileMetadata> listPublic(String tag, SortBy sortBy, SortDir sortDir, int page, int size) {
        List<FileMetadata> filtered = new ArrayList<>();
        for (FileMetadata f : byId.values()) {
            if (f.visibility() == Visibility.PUBLIC && f.status() == FileStatus.READY) {
                if (tag == null || hasTagIgnoreCase(f.tags(), tag)) {
                    filtered.add(f);
                }
            }
        }
        filtered.sort(makeComparator(sortBy, sortDir));
        return slice(filtered, page, size);
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, String tag, SortBy sortBy, SortDir sortDir, int page, int size) {
        List<FileMetadata> filtered = new ArrayList<>();
        for (FileMetadata f : byId.values()) {
            if (Objects.equals(f.ownerId(), ownerId)) {
                if (tag == null || hasTagIgnoreCase(f.tags(), tag)) {
                    filtered.add(f);
                }
            }
        }
        filtered.sort(makeComparator(sortBy, sortDir));
        return slice(filtered, page, size);
    }

    private static boolean hasTagIgnoreCase(List<String> tags, String want) {
        if (tags == null || tags.isEmpty()) return false;
        for (String t : tags) {
            if (t != null && t.equalsIgnoreCase(want)) return true;
        }
        return false;
    }

    private static Comparator<FileMetadata> makeComparator(SortBy sortBy, SortDir dir) {
        Comparator<FileMetadata> c;
        switch (sortBy) {
            case FILENAME -> c = Comparator.comparing(FileMetadata::filename, Comparator.nullsLast(String::compareToIgnoreCase));
            case UPLOAD_DATE -> c = Comparator.comparing(FileMetadata::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case TAG -> c = Comparator.comparing(
                    f -> firstLower(f.tags()),
                    Comparator.nullsLast(String::compareTo)
            );
            case CONTENT_TYPE -> c = Comparator.comparing(FileMetadata::contentType, Comparator.nullsLast(String::compareTo));
            case FILE_SIZE -> c = Comparator.comparingLong(FileMetadata::size);
            default -> c = Comparator.comparing(FileMetadata::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if (dir == SortDir.DESC) c = c.reversed();
        return c;
    }

    private static String firstLower(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        String t = tags.get(0);
        return t == null ? null : t.toLowerCase(Locale.ROOT);
        // This is only for ordering; real Mongo adapter uses tagsNorm field.
    }

    private static List<FileMetadata> slice(List<FileMetadata> list, int page, int size) {
        int from = Math.max(0, page) * Math.max(1, size);
        if (from >= list.size()) return Collections.emptyList();
        int to = Math.min(list.size(), from + Math.max(1, size));
        return list.subList(from, to);
    }

    private String newId() {
        // 24-hex-like id for convenience (not a real ObjectId)
        long n = seq.getAndIncrement();
        return String.format("%024x", n);
    }
}
