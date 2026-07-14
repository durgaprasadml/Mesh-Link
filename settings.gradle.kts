pluginManagement {
    repositories {
        google()                // 🔥 REQUIRED for Firebase plugins
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()                // 🔥 REQUIRED for Firebase dependencies
        mavenCentral()
    }
}

rootProject.name = "MeshLink"   // ⚠️ Removed space (IMPORTANT)
include(":app")
include(":benchmark")