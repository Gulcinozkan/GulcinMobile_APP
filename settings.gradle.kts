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
    plugins {
        id("org.jetbrains.kotlin.android") version "2.0.0" apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
        id("org.jetbrains.kotlin.jvm") version "2.0.0" apply false
        id("org.jetbrains.kotlin.kapt") version "2.0.0" apply false
        id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0" apply false
        id("org.jetbrains.kotlin.plugin.parcelize") version "2.0.0" apply false
        id("org.jetbrains.kotlin.plugin.compose.compiler") version "1.5.11" apply false
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}



rootProject.name = "GulcinMobile"
include(":app")
