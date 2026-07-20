plugins {
    kotlin("jvm")
    `java-library`
    application
}
version = "0.1.5"

application {
    mainClass.set("com.github.grishberg.cad3d.cli.CliRunnerKt")
}

dependencies {
    implementation(project(":javascad"))
    implementation(project(":plugin"))
    implementation(project(":kbd_core"))
    implementation(project(":common"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation(kotlin("test"))
}

tasks.register<Jar>("fatJar") {
    archiveBaseName.set("cli-runner")
    archiveVersion.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.github.grishberg.cad3d.cli.CliRunnerKt"
    }
    val runtimeCp = configurations.runtimeClasspath.get()
    from(runtimeCp.map(::zipTree))
    from(sourceSets.main.get().output.classesDirs)
    from(sourceSets.main.get().output.resourcesDir)
}

kotlin {
    jvmToolchain(17)
}
