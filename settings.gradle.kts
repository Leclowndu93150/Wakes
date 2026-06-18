pluginManagement {
    repositories {
        maven { url = uri("https://maven.leclowndu93150.dev/releases") }
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.neoforged.net/releases") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    id("dev.prism.settings") version "+"
}

rootProject.name = "Wakes"

prism {
    version("1.20.1") {
        forge()
    }
    version("1.21.1") {
        neoforge()
    }
    version("26.1.2") {
        common()
        fabric()
        neoforge()
    }
}
