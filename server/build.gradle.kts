
plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.nexus"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // REST API
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // Database communication
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    // Database migrations
    implementation("org.springframework.boot:spring-boot-starter-flyway")

    // Flyway compatability package for postgresql
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // PostgreSQL driver
    runtimeOnly("org.postgresql:postgresql")
}
