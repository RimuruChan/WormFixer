package com.github.rimuruchan

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class FixJarTask(
    private val jarFile: JarFile,
    private val target: File
) : Runnable {

    override fun run() {
        try {
            println("Processing ${target.absolutePath}")
            val repaired = File(target.absolutePath + ".0")
            if (repaired.exists()) repaired.delete()
            repaired.createNewFile()
            repaired.deleteOnExit()
            JarOutputStream(FileOutputStream(repaired)).use { jos ->
                val entryIter = jarFile.entries()
                while (entryIter.hasMoreElements()) {
                    val je = entryIter.nextElement()
                    val newJe = JarEntry(je.name).apply {
                        comment = je.comment
                        extra = je.extra
                    }
                    val entryName = je.name
                    if (entryName.startsWith("javassist/") ||
                        entryName.endsWith("L10.class") ||
                        entryName == ".l_ignore" ||
                        entryName == ".l1"
                    ) continue
                    if (je.isDirectory || !entryName.endsWith(
                            ".class"
                        ) || !Regex(".*module-info\\.class").matches(entryName)
                    ) {
                        jos.putNextEntry(newJe)
                        jarFile.getInputStream(je).use { `is` ->
                            `is`.copyTo(jos)
                        }
                        jos.closeEntry()
                    } else {
                        var modifiedClassBytes: ByteArray
                        try {
                            jarFile.getInputStream(je).use { `is` ->
                                val cr = ClassReader(`is`)
                                val cw = ClassWriter(cr, 0)
                                cr.accept(ClassTransformer(cw), 0)
                                modifiedClassBytes = cw.toByteArray()
                                jos.putNextEntry(newJe)
                            }
                        } catch (e: Throwable) {
                            println("Failed to modify $entryName in ${target.absolutePath}:\n${e.stackTraceToString()}")
                            jarFile.getInputStream(je).use { `is` ->
                                val os = ByteArrayOutputStream()
                                `is`.copyTo(os)
                                modifiedClassBytes = os.toByteArray()
                            }
                        }
                        jos.write(modifiedClassBytes)
                        jos.closeEntry()
                    }
                }
            }
            FileOutputStream(target).use { fos ->
                FileInputStream(repaired).use { fis ->
                    fis.copyTo(fos)
                }
            }
        } catch (e: Throwable) {
            println("Failed to repair ${target.absolutePath}:\n${e.stackTraceToString()}")
        }
    }
}
