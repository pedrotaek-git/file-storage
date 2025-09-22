package com.digitalarkcorp.filestorage;

import com.digitalarkcorp.filestorage.domain.ports.MetadataRepository;
import com.digitalarkcorp.filestorage.domain.ports.StoragePort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest
class FileStorageApplicationTests {

	@TestConfiguration
	static class TestDoublesConfig {
		@Bean
		MetadataRepository metadataRepository() {
			return Mockito.mock(MetadataRepository.class);
		}

		@Bean
		StoragePort storagePort() {
			return Mockito.mock(StoragePort.class);
		}
	}

	@Test
	void contextLoads() {
		// if the Spring context starts, we're good
	}
}
