package com.digitalarkcorp.filestorage.testdouble;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FakeMetadataRepository implements MetadataRepository {

    private final Map<String, FileMetadata> byId = new ConcurrentHashMap<>();

    @Override
    public FileMetadata save(FileMetadata m) {
        String id = m.id() != null ? m.id() : genId();
        FileMetadata saved = new FileMetadata(
                id,
                m.ownerId(),
                m.filename(),
                m.visibility(),
                m.tags(),
                m.size(),
                m.contentType(),
                m.contentHash(),
                m.linkId(),
                m.status(),
                m.createdAt(),
                m.updatedAt()
        );
        byId.put(id, saved);
        return saved;
    }

    @Override
    public FileMetadata findById(String id) {
        return byId.get(id);
    }

    @Override
    public FileMetadata findByLinkId(String linkId) {
        return byId.values().stream()
                .filter(f -> linkId != null && linkId.equals(f.linkId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean deleteByIdAndOwner(String id, String ownerId) {
        FileMetadata existing = byId.get(id);
        if (existing == null) return false;
        if (!existing.ownerId().equals(ownerId)) return false;
        byId.remove(id);
        return true;
    }

    @Override
    public boolean existsByOwnerAndContentHash(String ownerId, String contentHash) {
        return byId.values().stream()
                .anyMatch(f -> f.ownerId().equals(ownerId) && contentHash.equals(f.contentHash()));
    }

    @Override
    public boolean existsByOwnerAndFilename(String ownerId, String filename) {
        return byId.values().stream()
                .anyMatch(f -> f.ownerId().equals(ownerId) && f.filename().equals(filename));
    }

    @Override
    public void rename(String id, String newFilename, Instant now) {
        FileMetadata existing = byId.get(id);
        if (existing == null) return;
        FileMetadata renamed = new FileMetadata(
                existing.id(),
                existing.ownerId(),
                newFilename,
                existing.visibility(),
                existing.tags(),
                existing.size(),
                existing.contentType(),
                existing.contentHash(),
                existing.linkId(),
                existing.status(),
                existing.createdAt(),
                now != null ? now : existing.updatedAt()
        );
        byId.put(id, renamed);
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, ListQuery q) {
        List<FileMetadata> base = byId.values().stream()
                .filter(f -> f.ownerId().equals(ownerId))
                .collect(Collectors.toCollection(ArrayList::new));
        return applyQuery(base, q);
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery q) {
        List<FileMetadata> base = byId.values().stream()
                .filter(f -> f.visibility() == Visibility.PUBLIC)
                .collect(Collectors.toCollection(ArrayList::new));
        return applyQuery(base, q);
    }

    private static List<FileMetadata> applyQuery(List<FileMetadata> src, ListQuery q) {
        String tag = q.tag();
        String contains = containsValue(q);

        List<FileMetadata> filtered = src.stream()
                .filter(f -> tag == null || (f.tags() != null && f.tags().stream().anyMatch(t -> t.equalsIgnoreCase(tag))))
                .filter(f -> contains == null || f.filename().toLowerCase().contains(contains.toLowerCase()))
                .collect(Collectors.toCollection(ArrayList::new));

        Comparator<FileMetadata> cmp;
        switch (q.sortBy()) {
            case FILENAME -> cmp = Comparator.comparing(FileMetadata::filename, String.CASE_INSENSITIVE_ORDER);
            case SIZE -> cmp = Comparator.comparingLong(FileMetadata::size);
            case CREATED_AT -> cmp = Comparator.comparing(FileMetadata::createdAt);
            default -> cmp = Comparator.comparing(FileMetadata::createdAt);
        }
        if (q.sortDir() == ListQuery.SortDir.DESC) cmp = cmp.reversed();
        filtered.sort(cmp);

        int from = Math.max(0, q.page() * q.size());
        int to = Math.min(filtered.size(), from + q.size());
        if (from >= filtered.size()) return List.of();
        return filtered.subList(from, to);
    }

    private static String containsValue(ListQuery q) {
        for (String name : new String[]{"filenameContains", "q", "nameLike"}) {
            try {
                Method m = q.getClass().getMethod(name);
                Object v = m.invoke(q);
                return v != null ? String.valueOf(v) : null;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String genId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    @Override
    public long countByContentHash(String contentHash) {
        return byId.values().stream()
                .filter(f -> contentHash != null && contentHash.equals(f.contentHash()))
                .count();
    }

}
