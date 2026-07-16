plugins {
    kotlin("jvm")
    `java-library`
    application
}
version = "1.0.0"

application {
    mainClass.set("com.github.grishberg.scripting.cli.DslRunnerKt")
}

dependencies {
    implementation(project(":javascad"))
    implementation(project(":plugin"))

    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:1.8.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.8.0")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.8.0")

    testImplementation("junit:junit:4.13.1")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}
