group = "de.uniflitzer"
version = "0.0.1"

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.keycloak:keycloak-admin-client:26.0.3")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
	implementation("fi.solita.clamav:clamav-client:1.0.1")
	implementation("org.apache.tika:tika-core:3.0.0")
	implementation("org.apache.tika:tika-parsers-standard-package:3.0.0")
	implementation("net.coobird:thumbnailator:0.4.20")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.6"
	kotlin("plugin.jpa") version "1.9.25"
	id("idea") apply true
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

idea {
	module {
		isDownloadJavadoc = true
		isDownloadSources = true
	}
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}