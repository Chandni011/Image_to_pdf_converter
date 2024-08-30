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
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven-central.storage.apis.com")
        }
        ivy {
            url = uri("https://github.com/ivy-rep/")
        }
        mavenLocal()
        flatDir {
            dirs("libs")
        }
        gradlePluginPortal()
    }
}

rootProject.name = "Image to PDF Converter"
include(":app")
 