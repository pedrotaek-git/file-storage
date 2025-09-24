plugins {
	id("java")
	id("org.springframework.boot") version "3.4.0" // ou 3.3.x
	id("io.spring.dependency-management") version "1.1.6"
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.apache.tika:tika-core:2.9.0")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb") // << ESSENCIAL

	// Actuator (opcional)
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// MinIO
	implementation("io.minio:minio:8.5.10")

	// Lombok
	compileOnly("org.projectlombok:lombok:1.18.34")
	annotationProcessor("org.projectlombok:lombok:1.18.34")
	testCompileOnly("org.projectlombok:lombok:1.18.34")
	testAnnotationProcessor("org.projectlombok:lombok:1.18.34")

	// Testes
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mongodb")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.assertj:assertj-core:3.26.0")

}

tasks.withType<Test> {
	useJUnitPlatform()
}
