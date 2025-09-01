package com.github.grishberg.cad3d.plugins

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FileWatcherImpl(
    private val pluginsDir: File,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {

    private var isWatching = false
    // Храним хэши файлов для обнаружения изменений
    private val fileHashes = ConcurrentHashMap<String, String>()
    private var lastModified = pluginsDir.lastModified()
    private var job: Job? = null

    var onPluginFound: (File) -> Unit = { }

    fun startWatching() {
        job = scope.launch {
            while (isActive) {
                delay(2000) // Проверка каждые 2 секунды
                checkForChanges()
            }
        }
    }

    fun stopWatching() {
        job?.cancel()
        job = null
    }

    private fun checkForChanges() {
        val jarFiles = pluginsDir.listFiles { file ->
            file.extension.equals("jar", ignoreCase = true) && file.isFile
        } ?: emptyArray()

        jarFiles.forEach { jarFile ->
            val currentHash = calculateFileHash(jarFile)
            val previousHash = fileHashes[jarFile.absolutePath]

            if (previousHash == null || currentHash != previousHash) {
                fileHashes[jarFile.absolutePath] = currentHash
                onPluginFound(jarFile)
            }
        }

        // Проверяем удаленные файлы
        val iterator = fileHashes.iterator()
        while (iterator.hasNext()) {
            val (filePath, _) = iterator.next()
            if (!File(filePath).exists()) {
                iterator.remove()
                // Можно уведомить об удалении файла
                println("File removed: $filePath")
            }
        }
    }

    private fun calculateFileHash(file: File): String {
        return file.inputStream().use { input ->
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
