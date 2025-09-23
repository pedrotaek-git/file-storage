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
import java.util.stream.Collectors;

public class FakeMetadataRepository implements MetadataRepository {

    private final Map<String, FileMetadata> byId = new ConcurrentHashMap<>();
    private final Map<String, String> byLink = new ConcurrentHashMap<>();

    @Override
    public FileMetadata save(FileMetadata m) {
        FileMetadata toStore = m;
        if (toStore.id() == null || toStore.id().isBlank()) {
            toStore = new FileMetadata(
                    UUID.randomUUID().toString(),
                    m.ownerId(),
                    m.filename(),
                    m.visibility(),
                    m.tags(),
                    m.size(),
                    m.contentType(),
                    m.contentHash(),
                    m.linkId() == null ? UUID.randomUUID().toString() : m.linkId(),
                    m.status() == null ? FileStatus.READY : m.status(),
                    m.createdAt() == null ? Instant.now() : m.createdAt(),
                    m.updatedAt() == null ? Instant.now() : m.updatedAt()
            );
        }
        byId.put(toStore.id(), toStore);
        if (toStore.linkId() != null) {
            byLink.put(toStore.linkId(), toStore.id());
        }
        return toStore;
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
        String id = byLink.get(linkId);
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    @Override
    public void deleteById(String id) {
        FileMetadata removed = byId.remove(id);
        if (removed != null && removed.linkId() != null) {
            byLink.remove(removed.linkId());
        }
    }

    @Override
    public List<FileMetadata> listPublic(String tag, SortBy sortBy, SortDir sortDir, int page, int size) {
        return list(null, tag, sortBy, sortDir, page, size, true);
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, String tag, SortBy sortBy, SortDir sortDir, int page, int size) {
        return list(ownerId, tag, sortBy, sortDir, page, size, false);
    }

    private List<FileMetadata> list(String ownerId, String tag, SortBy sortBy, SortDir sortDir, int page, int size, boolean onlyPublic) {
        var base = byId.values().stream()
                .filter(f -> !onlyPublic || f.visibility() == Visibility.PUBLIC)
                .filter(f -> ownerId == null || Objects.equals(f.ownerId(), ownerId))
                .filter(f -> {
                    if (tag == null) return true;
                    var tags = f.tags();
                    if (tags == null || tags.isEmpty()) return false;
                    String t = tag.toLowerCase(Locale.ROOT);
                    return tags.stream().anyMatch(x -> x != null && x.toLowerCase(Locale.ROOT).equals(t));
                });

        Comparator<FileMetadata> cmp;
        switch (sortBy) {
            case FILENAME -> cmp = Comparator.comparing(FileMetadata::filename, Comparator.nullsFirst(String::compareToIgnoreCase));
            case UPLOAD_DATE -> cmp = Comparator.comparing(FileMetadata::createdAt, Comparator.nullsFirst(Comparator.naturalOrder()));
            case FILE_SIZE -> cmp = Comparator.comparingLong(FileMetadata::size);
            case CONTENT_TYPE -> cmp = Comparator.comparing(FileMetadata::contentType, Comparator.nullsFirst(String::compareToIgnoreCase));
            case TAG -> cmp = Comparator.comparing(
                    f -> {
                        var tags = f.tags();
                        return (tags == null || tags.isEmpty() || tags.get(0) == null) ? "" : tags.get(0).toLowerCase(Locale.ROOT);
                    },
                    Comparator.nullsFirst(String::compareTo)
            );
            default -> cmp = Comparator.comparing(FileMetadata::filename, Comparator.nullsFirst(String::compareToIgnoreCase));
        }
        if (sortDir == SortDir.DESC) cmp = cmp.reversed();

        var sorted = base.sorted(cmp).collect(Collectors.toList());
        int from = Math.min(page * size, sorted.size());
        int to = Math.min(from + size, sorted.size());
        return sorted.subList(from, to);
    }
}
