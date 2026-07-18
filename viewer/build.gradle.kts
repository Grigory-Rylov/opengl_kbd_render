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
    implementation(project(":scripting"))
    implementation(project(":kbd_core"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    implementation(files("libs/jogamp-fat.jar"))
    implementation(files("libs/jogl-all-2.5.0.jar"))
    implementation(files("libs/jogl-all-2.5.0-natives-macosx-universal.jar"))

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.github.grishberg.cad3d.viewer.Main")
    // Suppress JOGL AppContext reflective access warnings on JDK 17+
    applicationDefaultJvmArgs = listOf(
        "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.awt=ALL-UNNAMED"
    )
}

val nativesDir = layout.buildDirectory.dir("manifold-natives")

tasks.register<Copy>("extractManifoldNatives") {
    val jarFile = file("../libs/manifold3d-0.1.4.jar")
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
        val os = System.getProperty("os.name").lowercase()
        if (os.contains("nix") || os.contains("nux")) {
            val nativeDir = nativesDir.get().asFile
            if (nativeDir.exists()) {
                val soDir = detectNativeDir(nativeDir)
                if (soDir != null) {
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
}

kotlin {
    jvmToolchain(17) // Устанавливаем единую версию Java для всех задач
}

