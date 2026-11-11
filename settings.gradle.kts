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

// 仅本地 bionic 兼容构建才加入打桩 room-compiler 所在的本地仓库；标准/CI 构建不含。
val localBionic = (gradle.startParameter.projectProperties["wordstreak.localBionic"]
    ?: providers.gradleProperty("wordstreak.localBionic").orNull)?.toBoolean() ?: false

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (localBionic) {
            maven { url = uri("$rootDir/localrepo") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "WordStreak"
include(":app")
