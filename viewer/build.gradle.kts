plugins {
    kotlin("jvm")
    id("java")
    application
}

dependencies {
    implementation("com.github.kitakeyos-dev:plugin4j:v1.0.1")
    implementation(project(":cad3d"))
    implementation(project(":javascad"))
    implementation(project(":plugin"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    implementation("org.jogamp.gluegen:gluegen-rt-main:2.5.0")
    implementation(files("libs/jogl-all-2.5.0.jar"))
    implementation(files("libs/jogl-all-2.5.0-natives-macosx-universal.jar"))

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("org.example.viewer.MainKt")
}

kotlin {
    jvmToolchain(17) // Устанавливаем единую версию Java для всех задач
}
