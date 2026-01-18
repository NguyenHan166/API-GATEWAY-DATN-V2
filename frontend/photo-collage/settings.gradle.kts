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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack repository for UCrop
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://artifact.bytedance.com/repository/pangle/")
        }
        maven {
            url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
        }
        maven {
            url = uri("https://android-sdk.is.com/")
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/dunght196/minsap-adsdk")
            credentials {
                username = settings.providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("USERNAME")
                password = settings.providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("TOKEN")
            }
        }
    }
}

rootProject.name = "photo-collage"
include(":app")
include(":translator")
include(":asset_photo")
include(":ucrop")
