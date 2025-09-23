package com.digitalarkcorp.filestorage.testdouble;

import com.digitalarkcorp.filestorage.api.dto.ListQuery;
import com.digitalarkcorp.filestorage.api.errors.ConflictException;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.Visibility;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class FakeMetadataRepository implements MetadataRepository {
    private final Map<String, FileMetadata> byId = new ConcurrentHashMap<>();
    private final Map<String, String> nameKey = new ConcurrentHashMap<>();
    private final Map<String, String> hashKey = new ConcurrentHashMap<>();

    @Override
    public FileMetadata save(FileMetadata m) {
        String id = m.id() == null ? UUID.randomUUID().toString().replace("-", "") : m.id();
        Instant now = Instant.now();
        FileMetadata toSave = new FileMetadata(
                id, m.ownerId(), m.filename(), m.visibility(), m.tags(), m.size(),
                m.contentType(), m.contentHash(), m.linkId(), m.status(),
                m.createdAt() == null ? now : m.createdAt(), now
        );

        String nk = m.ownerId() + "|" + m.filename();
        String hk = m.ownerId() + "|" + m.contentHash();

        String existingNameId = nameKey.get(nk);
        if (existingNameId != null && !existingNameId.equals(id)) throw new ConflictException("duplicate filename for owner");
        nameKey.put(nk, id);

        String existingHashId = hashKey.get(hk);
        if (existingHashId != null && !existingHashId.equals(id)) throw new ConflictException("duplicate content for owner");
        hashKey.put(hk, id);

        byId.put(id, toSave);
        return toSave;
    }

    @Override
    public boolean existsByOwnerAndFilename(String ownerId, String filename) {
        return nameKey.containsKey(ownerId + "|" + filename);
    }

    @Override
    public boolean existsByOwnerAndContentHash(String ownerId, String contentHash) {
        return hashKey.containsKey(ownerId + "|" + contentHash);
    }

    @Override
    public FileMetadata findById(String id) {
        return byId.get(id);
    }

    @Override
    public FileMetadata findByLinkId(String linkId) {
        return byId.values().stream().filter(m -> Objects.equals(m.linkId(), linkId)).findFirst().orElse(null);
    }

    @Override
    public void rename(String id, String newFilename, Instant now) {
        FileMetadata cur = byId.get(id);
        if (cur == null) return;

        String nk = cur.ownerId() + "|" + newFilename;
        String exists = nameKey.get(nk);
        if (exists != null && !exists.equals(id)) throw new ConflictException("duplicate filename for owner");

        nameKey.remove(cur.ownerId() + "|" + cur.filename());
        nameKey.put(nk, id);

        FileMetadata updated = new FileMetadata(
                cur.id(), cur.ownerId(), newFilename, cur.visibility(), cur.tags(),
                cur.size(), cur.contentType(), cur.contentHash(), cur.linkId(),
                cur.status(), cur.createdAt(), now
        );
        byId.put(id, updated);
    }

    @Override
    public boolean deleteByIdAndOwner(String id, String ownerId) {
        FileMetadata cur = byId.get(id);
        if (cur == null || !Objects.equals(cur.ownerId(), ownerId)) return false;
        byId.remove(id);
        nameKey.remove(ownerId + "|" + cur.filename());
        hashKey.remove(ownerId + "|" + cur.contentHash());
        return true;
    }

    @Override
    public List<FileMetadata> listByOwner(String ownerId, ListQuery q) {
        return filter(byId.values().stream().filter(m -> Objects.equals(m.ownerId(), ownerId)), q);
    }

    @Override
    public List<FileMetadata> listPublic(ListQuery q) {
        return filter(byId.values().stream().filter(m -> m.visibility() == Visibility.PUBLIC), q);
    }

    private static List<FileMetadata> filter(Stream<FileMetadata> stream, ListQuery q) {
        String tag = q.tag();
        String nameLike = q.filenameContains();
        if (tag != null && !tag.isBlank()) {
            String t = tag.toLowerCase();
            stream = stream.filter(m -> m.tags() != null && m.tags().stream().anyMatch(x -> x != null && x.equalsIgnoreCase(t)));
        }
        if (nameLike != null && !nameLike.isBlank()) {
            String n = nameLike.toLowerCase();
            stream = stream.filter(m -> m.filename() != null && m.filename().toLowerCase().contains(n));
        }
        Comparator<FileMetadata> cmp;
        switch (q.sortBy()) {
            case CREATED_AT -> cmp = Comparator.comparing(FileMetadata::createdAt);
            case SIZE -> cmp = Comparator.comparingLong(FileMetadata::size);
            default -> cmp = Comparator.comparing(FileMetadata::filename, Comparator.nullsFirst(String::compareTo));
        }
        if (q.sortDir() == ListQuery.SortDir.DESC) cmp = cmp.reversed();
        var all = stream.sorted(cmp).toList();
        int from = Math.max(0, q.page() * q.size());
        int to = Math.min(all.size(), from + q.size());
        if (from >= to) return List.of();
        return all.subList(from, to);
    }
}
