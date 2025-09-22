package com.digitalarkcorp.filestorage;

import com.digitalarkcorp.filestorage.api.dto.SortBy;
import com.digitalarkcorp.filestorage.api.dto.SortDir;
import com.digitalarkcorp.filestorage.domain.FileMetadata;
import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
public class FileStorageApplicationTests {

	@Test
	public void contextLoads() {
		// context boot sanity check
	}

	@TestConfiguration
	static class TestDoublesConfig {

		@Bean
		@Primary
		MetadataRepository metadataRepositoryDouble() {
			return new MetadataRepository() {
				@Override public FileMetadata save(FileMetadata m) { return m; }
				@Override public Optional<FileMetadata> findById(String id) { return Optional.empty(); }
				@Override public Optional<FileMetadata> findByOwnerAndFilename(String ownerId, String filename) { return Optional.empty(); }
				@Override public Optional<FileMetadata> findByOwnerAndContentHash(String ownerId, String contentHash) { return Optional.empty(); }
				@Override public Optional<FileMetadata> findByLinkId(String linkId) { return Optional.empty(); }
				@Override public void deleteById(String id) { }
				@Override public List<FileMetadata> listPublic(String tag, SortBy sortBy, SortDir sortDir, int page, int size) { return List.of(); }
				@Override public List<FileMetadata> listByOwner(String ownerId, String tag, SortBy sortBy, SortDir sortDir, int page, int size) { return List.of(); }
			};
		}

		@Bean
		@Primary
		StoragePort storagePortDouble() {
			return new StoragePort() {
				@Override public String initiate(String objectKey) { return "test-upload"; }
				@Override public void uploadPart(String uploadId, int partNumber, InputStream data, long size) { }
				@Override public void complete(String uploadId) { }
				@Override public InputStream get(String objectKey) { return new ByteArrayInputStream(new byte[0]); }
				@Override public void delete(String objectKey) { }
			};
		}
	}
}
