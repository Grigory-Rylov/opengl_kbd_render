plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "Ergonomic_kbd_generator"

include("viewer", "cad3d", "javascad", "plugin", "kbd_core", "common")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://jogamp.org/deployment/maven/")
        }
        flatDir {
            dirs("libs")
        }
    }
}
