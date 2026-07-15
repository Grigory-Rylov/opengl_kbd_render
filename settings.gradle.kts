plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "Ergonomic_kbd_generator"

include("viewer", "cad3d", "javascad", "plugin", "kbd_core", "common")

includeBuild("libs/manifold3d-java/bindings/java")
// Когда JitPack заработает, заменить на:
// implementation("com.github.Grigory-Rylov:manifold3d-java:<tag>")
// и удалить includeBuild + submodule

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://jogamp.org/deployment/maven/")
        }
    }
}
