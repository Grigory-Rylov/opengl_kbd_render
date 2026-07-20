plugins {
    kotlin("jvm")
    `java-library`
}
version = "0.1.5"

dependencies {
    implementation(project(":javascad"))
    implementation(project(":plugin"))
    implementation(project(":kbd_core"))

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}
