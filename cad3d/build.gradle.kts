plugins {
    kotlin("jvm")
    `java-library`
    kotlin("plugin.serialization") version "1.9.0"
}
version = "1.0.1"

dependencies {
    implementation(project(":javascad"))
    implementation(project(":plugin"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17) // Устанавливаем единую версию Java для всех задач
}
