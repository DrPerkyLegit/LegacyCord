plugins {
    id("java")
    id("application")
}

group = "dev.drperky"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies { }


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "dev.drperky.LegacyCord"
    }
}