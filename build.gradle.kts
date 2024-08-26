import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    id("java")
    //application
}

group = "org.example"
version = "1.0-SNAPSHOT"

dependencies {
// https://mvnrepository.com/artifact/org.jogamp.jogl/jogl-all
    //implementation("org.jogamp.jogl:jogl-all:2.3.2")
    implementation (files("libs/jogamp-fat.jar"))
    implementation("org.jogamp.gluegen:gluegen-rt-main:2.3.2")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("stdlib-jdk8"))
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
