plugins {
    application
    java
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
    implementation("tools.jackson.core:jackson-databind:3.2.4")
}

application {
    mainClass.set("com.nexus.agent.NexusAgentApplication")
}