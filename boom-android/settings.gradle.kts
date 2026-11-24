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
    }
}

rootProject.name = "boom-android"

include(":app")
include(":core:model")
include(":core:designsystem")
include(":core:network")
include(":core:database")
include(":core:connectivity")
include(":core:audio")
include(":core:security")

include(":feature:auth")
include(":feature:welcome")
include(":feature:dashboard")
include(":feature:party")
include(":feature:chat")
include(":feature:activities")
include(":feature:guardian")
