package com.digitalarkcorp.filestorage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestConfig.class)
public class FileStorageApplicationTests {

	@Test
	public void contextLoads() {
		assertNotNull(FileStorageApplication.class);
	}
}
