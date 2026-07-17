package eu.printingin3d.javascad.manifold

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile

/**
 * On macOS ARM64, the manifold3d native dylibs use @rpath for inter-library references:
 *   libmanifold_jni.dylib  → @rpath/libmanifoldc.3.dylib
 *   libmanifoldc.3.dylib   → @rpath/libmanifold.3.dylib
 *
 * When ManifoldBindings extracts to a temp dir and calls System.load(), @rpath is not set
 * and the dynamic linker crashes. This utility extracts the dylibs, patches @rpath → @loader_path
 * via install_name_tool, and returns the directory so ManifoldBindings can load from it.
 */
object MacDylibFixer {

    private val isMacArm: Boolean by lazy {
        val os = System.getProperty("os.name", "").lowercase()
        val arch = System.getProperty("os.arch", "").lowercase()
        os.contains("mac") && (arch.contains("aarch") || arch.contains("arm"))
    }

    /**
     * If on macOS ARM64, extracts dylibs, patches them, and returns the directory.
     * Otherwise returns null — ManifoldBindings should use default path.
     */
    fun prepareDylibs(): File? {
        if (!isMacArm) return null

        val tmpDir = Files.createTempDirectory("manifold_mac").toFile().apply {
            deleteOnExit()
        }

        val jarEntry = JarLocator.findManifoldJar() ?: return null

        val libs = arrayOf(
            "libmanifold.dylib",
            "libmanifold.3.dylib",
            "libmanifoldc.dylib",
            "libmanifoldc.3.dylib",
            "libmanifold_jni.dylib",
        )

        ZipFile(jarEntry).use { zf ->
            for (lib in libs) {
                val entryName = "manifold3d/natives/mac-arm64/$lib"
                val entry = zf.getEntry(entryName) ?: continue
                val target = File(tmpDir, lib)
                zf.getInputStream(entry).use { inStream ->
                    Files.copy(inStream, target.toPath())
                }
                println("Extracted to libs/: $lib")
            }
        }

        // Patch @rpath → @loader_path
        val patches = listOf(
            Triple("$tmpDir/libmanifold_jni.dylib", "@rpath/libmanifoldc.3.dylib", "@loader_path/libmanifoldc.3.dylib"),
            Triple("$tmpDir/libmanifoldc.3.dylib", "@rpath/libmanifold.3.dylib", "@loader_path/libmanifold.3.dylib"),
        )

        for ((targetLib, oldId, newId) in patches) {
            val f = File(targetLib)
            if (f.exists()) {
                val cmd = arrayOf("install_name_tool", "-change", oldId, newId, targetLib)
                try {
                    val proc = ProcessBuilder(*cmd).start()
                    proc.waitFor()
                    if (proc.exitValue() == 0) {
                        println("Patched: $targetLib  $oldId → $newId")
                    } else {
                        println("WARNING: install_name_tool failed for $targetLib (exit ${proc.exitValue()})")
                    }
                } catch (e: Exception) {
                    println("WARNING: install_name_tool not available: ${e.message}")
                }
            }
        }

        return tmpDir
    }
}

private object JarLocator {

    fun findManifoldJar(): File? {
        // Try from the classloader resource
        val resource =
            javaClass.classLoader.getResource("manifold3d/natives/mac-arm64/libmanifold_jni.dylib")
        if (resource != null) {
            // The resource is inside a jar; extract the jar path
            val uri = resource.toExternalForm()
            if (uri.startsWith("jar:file:")) {
                val jarUrl = uri.substring("jar:".length).substringBefore("!/")
                return File(jarUrl.removePrefix("file:"))
            }
        }

        // Fallback: look in libs/ directory
        val libs = File("libs/manifold3d-0.1.4.jar")
        if (libs.exists()) return libs

        return null
    }
}
