package com.github.rimuruchan

import java.io.File
import java.util.concurrent.Executors
import java.util.jar.JarFile

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: repair.jar <Target directory>" }
        val executor = Executors.newFixedThreadPool(maxOf(2, Runtime.getRuntime().availableProcessors() - 2))
        File(args[0]).walkTopDown()
            .filter { it.isFile && it.extension == "jar" }
            .forEach { file ->
                runCatching {
                    val jf = JarFile(file)
                    val fixJarTask = FixJarTask(jf, file)
                    println("Queued ${file.absolutePath}.")
                    executor.submit(fixJarTask)
                }.onFailure {
                    println("Failed to queue ${file.absolutePath}: ${it.message}")
                }
            }
        executor.shutdown()
    }
}
