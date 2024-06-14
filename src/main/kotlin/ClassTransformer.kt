package com.github.rimuruchan

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.util.*

class ClassTransformer(
    classVisitor: ClassVisitor
) : ClassVisitor(Opcodes.ASM9, classVisitor) {
    private var shouldModify = false
    private var className: String? = null

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        if (superName != null) {
            shouldModify = superName == "org/bukkit/plugin/java/JavaPlugin"
            className = name
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        return if (shouldModify && name == "onEnable") {
            MethodTransformer(super.visitMethod(access, name, descriptor, signature, exceptions), className!!)
        } else {
            super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }

    class MethodTransformer(
        methodVisitor: MethodVisitor,
        private val className: String
    ) : MethodVisitor(Opcodes.ASM9, methodVisitor) {
        private var del = false

        override fun visitVarInsn(opcode: Int, `var`: Int) {
            if (del) return
            super.visitVarInsn(opcode, `var`)
        }

        override fun visitInsn(opcode: Int) {
            if (del) return
            super.visitInsn(opcode)
        }

        override fun visitTypeInsn(opcode: Int, type: String) {
            if (opcode == Opcodes.NEW) {
                val lowerType = type.lowercase(Locale.getDefault())
                val pattern = Regex("^${className.lowercase(Locale.getDefault())}l\\d+$")
                if (pattern.matches(lowerType)) {
                    del = true
                    return
                }
            }
            super.visitTypeInsn(opcode, type)
        }

        override fun visitMethodInsn(
            opcode: Int,
            owner: String,
            name: String,
            descriptor: String,
            isInterface: Boolean
        ) {
            if (opcode == Opcodes.INVOKEVIRTUAL && name == "a") {
                val lowerOwner = owner.lowercase(Locale.getDefault())
                val pattern = Regex("^${className.lowercase(Locale.getDefault())}l\\d+$")
                if (pattern.matches(lowerOwner)) {
                    del = false
                    return
                }
            }
            if (del) return
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
    }
}
