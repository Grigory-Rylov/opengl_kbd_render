rootProject.name = "smooth_surface_opengl"

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
