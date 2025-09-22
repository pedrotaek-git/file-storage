plugins {
	java
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.digitalarkcorp"
version = "0.0.1-SNAPSHOT"
description = "File storage REST API (no UI) with large-file uploads, public/private visibility, unguessable download links, deduplication, tagging, sorting, and pagination. Java 17 + Spring Boot 3, MongoDB (metadata), S3/MinIO (objects), Docker, CI."

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
