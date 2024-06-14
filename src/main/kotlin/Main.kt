package com.github.rimuruchan

import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.jar.JarFile

object Main {
    private lateinit var executor: ExecutorService

    @JvmStatic
    fun main(args: Array<out String>) {
        require(args.size == 1) { "Usage: repair.jar <Target directory>" }
        val threadCount = Runtime.getRuntime().availableProcessors()
        executor = Executors.newFixedThreadPool(maxOf(1, threadCount - 2))
        val selectingFolder = File(args[0])
        selectingFolder.walkTopDown().filter { it.isFile && it.extension == "jar" }.forEach {
            try {
                val jf = JarFile(it)
                val fixJarTask = FixJarTask(jf, it)
                println("Queued ${it.absolutePath}")
                executor.submit(fixJarTask)
            } catch (e: Throwable) {
                println("Failed to queue ${it.absolutePath}:\n${e.stackTraceToString()}")
            }
        }
        executor.shutdown()
    }
}
