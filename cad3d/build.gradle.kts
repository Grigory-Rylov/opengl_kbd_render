plugins {
    kotlin("jvm")
    `java-library`
    application
}
version = "1.0.1"

application {
    mainClass.set("com.github.grishberg.cad3d.cli.CliRunnerKt")
}

val nativesDir = layout.buildDirectory.dir("manifold-natives")

tasks.register<Copy>("extractManifoldNatives") {
    val jarFile = file("../libs/manifold3d-3.2.13.jar")
    from(zipTree(jarFile)) {
        include("manifold3d/natives/**/libmanifold*.so*")
        include("manifold3d/natives/**/libmanifold*.dylib*")
    }
    into(nativesDir)
}

fun detectNativeDir(baseDir: File): File? {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val prefix = if (os.contains("mac")) "mac-" else if (os.contains("win")) "win-" else "linux-"
    val suffix = when {
        arch.contains("aarch64") || arch.contains("arm64") -> "arm64"
        arch.contains("amd64") || arch.contains("x86_64") -> "x86_64"
        else -> null
    }
    return baseDir.walkTopDown().find { dir ->
        dir.name == "${prefix}${suffix}" && dir.listFiles()?.any { f -> f.name.startsWith("libmanifold") } == true
    }
}

tasks.withType<JavaExec>().configureEach {
    dependsOn("extractManifoldNatives")
    doFirst {
        val nativeDir = nativesDir.get().asFile
        if (nativeDir.exists()) {
            val soDir = detectNativeDir(nativeDir)
            if (soDir != null) {
                // LD_PRELOAD forces these libraries to be loaded BEFORE the JVM,
                // so all symbols are available globally
                val preloads = listOf(
                    File(soDir, "libmanifold.so.3"),
                    File(soDir, "libmanifoldc.so.3")
                ).filter { it.exists() }.map { it.absolutePath }.joinToString(":")
                if (preloads.isNotEmpty()) {
                    environment("LD_PRELOAD", preloads)
                }
            }
        }
    }
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
