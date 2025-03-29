import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    id("java")
    kotlin("plugin.serialization") version "1.9.0"
    id("org.bytedeco.gradle-javacpp-platform") version "1.5.9"
    //application
}

group = "org.example"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    // https://mvnrepository.com/artifact/org.jogamp.jogl/jogl-all
    //implementation (files("libs/jogamp-fat.jar"))

    implementation("org.jogamp.gluegen:gluegen-rt-main:2.5.0")
//    implementation("org.jogamp.jogl:jogl-all:2.5.0")
    implementation (files("libs/jogl-all-2.5.0.jar"))
    implementation (files("libs/jogl-all-2.5.0-natives-macosx-universal.jar"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}
/*
application {
    mainClass.set("MainKt")
}
*/

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
}
