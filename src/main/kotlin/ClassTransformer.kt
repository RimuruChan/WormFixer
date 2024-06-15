package com.github.rimuruchan

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ClassTransformer(
    classVisitor: ClassVisitor
) : ClassVisitor(Opcodes.ASM9, classVisitor) {
    var isModified = false
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
            MethodTransformer(super.visitMethod(access, name, descriptor, signature, exceptions), this)
        } else {
            super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }

    class MethodTransformer(
        methodVisitor: MethodVisitor,
        private val classTransformer: ClassTransformer
    ) : MethodVisitor(Opcodes.ASM9, methodVisitor) {
        private var del = false
        private val className = classTransformer.className!!

        override fun visitVarInsn(opcode: Int, `var`: Int) {
            if (!del) super.visitVarInsn(opcode, `var`)
        }

        override fun visitInsn(opcode: Int) {
            if (!del) super.visitInsn(opcode)
        }

        override fun visitTypeInsn(opcode: Int, type: String) {
            if (opcode == Opcodes.NEW && "^${className.lowercase()}l\\d+$".toRegex().matches(type.lowercase())) {
                classTransformer.isModified = true
                del = true
                return
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
            if (opcode == Opcodes.INVOKEVIRTUAL && name == "a" && "^${className.lowercase()}l\\d+$".toRegex()
                    .matches(owner.lowercase())
            ) {
                classTransformer.isModified = true
                del = false
                return
            }
            if (!del) super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
    }
}
