package com.digitalarkcorp.filestorage.testdouble;

import com.digitalarkcorp.filestorage.api.dto.SortBy;
import com.digitalarkcorp.filestorage.api.dto.SortDir;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.FileStatus;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryMetadataRepository implements MetadataRepository {
    private final Map<String, FileMetadata> db = new ConcurrentHashMap<>();

    @Override
    public FileMetadata save(FileMetadata m) {
        String id = (m.id() == null) ? UUID.randomUUID().toString().replace("-", "").substring(0, 24) : m.id();
        FileMetadata saved = new FileMetadata(
                id,
                m.ownerId(),
                m.filename(),
                m.visibility(),
                m.tags(),
                m.size(),
                m.contentType(),
                m.objectKey(),
                m.linkId(),
                m.status(),
                m.createdAt(),
                m.updatedAt(),
                m.contentHash()
        );
        db.put(id, saved);
        return saved;
    }

    @Override
    public Optional<FileMetadata> findById(String id) {
        return Optional.ofNullable(db.get(id));
    }

    @Override
    public Optional<FileMetadata> findByOwnerAndFilename(String ownerId, String filename) {
        return db.values().stream()
                .filter(x -> x.ownerId().equals(ownerId) && x.filename().equals(filename))
                .findFirst();
    }

    @Override
    public Optional<FileMetadata> findByOwnerAndContentHash(String ownerId, String contentHash) {
        return db.values().stream()
                .filter(x -> x.ownerId().equals(ownerId) && Objects.equals(x.contentHash(), contentHash))
                .findFirst();
    }

    @Override
    public Optional<FileMetadata> findByLinkId(String linkId) {
        return db.values().stream().filter(x -> x.linkId().equals(linkId)).findFirst();
    }

    @Override
    public void deleteById(String id) {
        db.remove(id);
    }

    @Override
    public List<FileMetadata> listPublic(String tag, SortBy sortBy, SortDir sortDir, int page, int size) {
        return listBy(o -> o.visibility() == Visibility.PUBLIC && o.status() == FileStatus.READY, tag, sortBy, sortDir, page, size);
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, String tag, SortBy sortBy, SortDir sortDir, int page, int size) {
        return listBy(o -> o.ownerId().equals(ownerId), tag, sortBy, sortDir, page, size);
    }

    private List<FileMetadata> listBy(java.util.function.Predicate<FileMetadata> baseFilter,
                                      String tag, SortBy sortBy, SortDir sortDir, int page, int size) {
        List<FileMetadata> filtered = db.values().stream()
                .filter(baseFilter)
                .filter(o -> tag == null || (o.tags() != null && o.tags().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .anyMatch(t -> t.equals(tag.toLowerCase()))))
                .collect(Collectors.toList());

        Comparator<FileMetadata> cmp = switch (sortBy) {
            case FILENAME     -> Comparator.comparing(FileMetadata::filename, Comparator.nullsLast(String::compareTo));
            case UPLOAD_DATE  -> Comparator.comparing(FileMetadata::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case TAG          -> Comparator.comparing(o -> firstTag(o.tags()), Comparator.nullsLast(String::compareTo));
            case CONTENT_TYPE -> Comparator.comparing(FileMetadata::contentType, Comparator.nullsLast(String::compareTo));
            case FILE_SIZE    -> Comparator.comparingLong(FileMetadata::size);
        };
        if (sortDir == SortDir.DESC) cmp = cmp.reversed();

        filtered.sort(cmp);

        int from = Math.max(page * size, 0);
        int to = Math.min(from + size, filtered.size());
        if (from >= to) return List.of();
        return filtered.subList(from, to);
    }

    private String firstTag(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return tags.get(0);
    }
}
