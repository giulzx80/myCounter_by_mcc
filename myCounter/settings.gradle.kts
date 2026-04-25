// Configurazione Gradle a livello di progetto - allineata allo stile MyVote_by_mcc
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Risoluzione automatica della toolchain JVM via foojay (necessario per gradle-daemon-jvm)
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Repository necessario per MPAndroidChart (PhilJay)
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "myCounter by MCC"
include(":app")
