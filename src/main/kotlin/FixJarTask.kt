package com.github.rimuruchan

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class FixJarTask(
    private val jarFile: JarFile,
    private val target: File
) : Runnable {

    override fun run() {
        runCatching {
            var modified = false

            val repaired = File(target.absolutePath + ".0").apply {
                if (exists()) delete()
                createNewFile()
                deleteOnExit()
            }

            JarOutputStream(repaired.outputStream()).use { jos ->
                jarFile.entries().asSequence().forEach { je ->
                    val newJe = je.clone() as ZipEntry
                    val entryName = je.name
                    if (entryName.startsWith("javassist/") ||
                        entryName.endsWith("L10.class") ||
                        entryName == ".l_ignore" ||
                        entryName == ".l1"
                    ) return@forEach

                    jos.putNextEntry(newJe)
                    if (je.isDirectory || !entryName.endsWith(".class")) {
                        jos.write(jarFile.getInputStream(je).readBytes())
                        jos.closeEntry()
                        return@forEach
                    }

                    var classBytes: ByteArray = jarFile.getInputStream(je).readBytes()
                    runCatching {
                        val cr = ClassReader(classBytes)
                        val cw = ClassWriter(cr, 0)
                        val tr = ClassTransformer(cw)
                        cr.accept(tr, 0)
                        if (tr.isModified) {
                            modified = true
                            classBytes = cw.toByteArray()
                            println("Detected worm in $entryName in ${target.absolutePath}, repaired.")
                        }
                    }.onFailure {
                        println("Failed to modify $entryName in ${target.absolutePath}: ${it.message}")
                    }
                    jos.write(classBytes)
                    jos.closeEntry()
                }
            }
            jarFile.close()
            if (modified) {
                target.delete()
                repaired.renameTo(target)
                println("Repaired ${target.absolutePath}.")
            } else {
                repaired.delete()
            }
        }.onFailure {
            println("Failed to repair ${target.absolutePath}: ${it.message}")
        }
    }
}
