plugins {
    kotlin("jvm")
    `java-library`
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

kotlin {
    jvmToolchain(17) // Устанавливаем единую версию Java для всех задач
}
