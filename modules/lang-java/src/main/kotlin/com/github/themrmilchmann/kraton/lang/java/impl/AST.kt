/*
 * Copyright (c) 2017 Leon Linhart,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.themrmilchmann.kraton.lang.java.impl

import com.github.themrmilchmann.kraton.lang.java.impl.model.*
import com.github.themrmilchmann.kraton.lang.jvm.*
import java.util.*

const val IMPORT_WILDCARD = "*"

abstract class BodyMemberDeclaration

internal open class GroupDeclaration(
    open val sortingRule: Comparator<BodyMemberDeclaration>?,
    open val bodyMembers: MutableList<BodyMemberDeclaration> = mutableListOf()
) : BodyMemberDeclaration()

internal abstract class CompilationUnit {

    abstract val importDeclarations: MutableMap<String, MutableMap<String, ImportDeclaration>>

    fun isImported(type: IJvmType) =
        importDeclarations[type.packageName]?.any { it.value.member === IMPORT_WILDCARD || it.value.member == type.className } ?: false

    abstract fun isResolved(type: IJvmType): Boolean

    abstract fun import(container: String, member: String, mode: ImportType?, isStatic: Boolean, isImplicit: Boolean)

}

internal class OrdinaryCompilationUnit(
    val packageDeclaration: PackageDeclaration?,
    override val importDeclarations: MutableMap<String, MutableMap<String, ImportDeclaration>>,
    val typeDeclaration: TypeDeclaration
) : CompilationUnit() {

    init {
        importDeclarations.put("java.lang", mutableMapOf("*" to ImportDeclaration("java.lang", "*", isImplicit = true)))
    }

    constructor(packageDeclaration: PackageDeclaration, typeDeclaration: TypeDeclaration):
        this(packageDeclaration, mutableMapOf(), typeDeclaration)

    override fun isResolved(type: IJvmType) =
        isImported(type) || (packageDeclaration!!.equalsPackage(type))

    override fun import(
        container: String,
        member: String,
        mode: ImportType?,
        isStatic: Boolean,
        isImplicit: Boolean
    ) {
        if (mode == null) {
            if (container == packageDeclaration!!.identifiers.toQualifiedString()) return
            if (importDeclarations.flatMap { it.value.values }.map { "${it.container}.${it.member}" }.any { it == "$container.$member" || it == "$container.*" }) return
        }

        if (member != IMPORT_WILDCARD && importDeclarations.any { it.value.any { it.key == member } }) return

        val containerImports = importDeclarations.getOrPut(container, ::mutableMapOf)
        if (member in containerImports && mode === null) return

        val factory = { mem: String -> ImportDeclaration(container, mem, mode, isStatic, isImplicit) }

        if (mode === ImportType.QUALIFIED) {
            containerImports[member] = factory.invoke(member)
        } else if (IMPORT_WILDCARD !in containerImports) {
            val filteredImports = containerImports.filter { it.value.mode === null }

            if (filteredImports.size >= 2 || mode === ImportType.WILDCARD) {
                filteredImports.forEach { containerImports.remove(it.key) }
                containerImports[IMPORT_WILDCARD] = factory.invoke(IMPORT_WILDCARD)
            } else
                containerImports[member] = factory.invoke(member)
        }
    }

}

internal abstract class TypeDeclaration(
    val annotations: MutableList<Annotation>,
    val modifiers: MutableList<Modifiers>,
    sortingRule: Comparator<BodyMemberDeclaration>?,
    bodyMembers: MutableList<BodyMemberDeclaration>
) : GroupDeclaration(sortingRule, bodyMembers), DocumentedDeclaration

internal class PackageDeclaration(
    val identifiers: MutableList<String>
) {

    fun equalsPackage(type: IJvmType) =
        identifiers.toQualifiedString() == type.packageName

}

internal abstract class ClassDeclaration(
    annotations: MutableList<Annotation>,
    modifiers: MutableList<Modifiers>,
    sortingRule: Comparator<BodyMemberDeclaration>?,
    val superInterfaces: MutableList<IJvmType>,
    bodyMembers: MutableList<BodyMemberDeclaration>
) : TypeDeclaration(annotations, modifiers, sortingRule, bodyMembers)

internal class NormalClassDeclaration(
    annotations: MutableList<Annotation>,
    modifiers: MutableList<Modifiers>,
    val identifier: String,
    val typeParameters: MutableList<TypeParameter>,
    var superClass: IJvmType?,
    superInterfaces: MutableList<IJvmType>,
    override val sortingRule: Comparator<BodyMemberDeclaration>?,
    override val bodyMembers: MutableList<BodyMemberDeclaration>
) : ClassDeclaration(annotations, modifiers, sortingRule, superInterfaces, bodyMembers), DocumentedDeclaration {

    constructor(identifier: String):
        this(mutableListOf(), mutableListOf(), identifier, mutableListOf(), null, mutableListOf(), null, mutableListOf())

    override val documentation = Documentation()

}

internal class EnumClassDeclaration(
    annotations: MutableList<Annotation>,
    modifiers: MutableList<Modifiers>,
    val identifier: String,
    superInterfaces: MutableList<IJvmType>,
    val values: MutableList<EnumConstant>,
    override val sortingRule: Comparator<BodyMemberDeclaration>?,
    override val bodyMembers: MutableList<BodyMemberDeclaration>
) : ClassDeclaration(annotations, modifiers, sortingRule, superInterfaces, bodyMembers) {

    constructor(identifier: String):
        this(mutableListOf(), mutableListOf(), identifier, mutableListOf(), mutableListOf(), null, mutableListOf())

    override val documentation = Documentation()

}

internal abstract class InterfaceDeclaration(
    annotations: MutableList<Annotation>,
    modifiers: MutableList<Modifiers>,
    sortingRule: Comparator<BodyMemberDeclaration>?,
    bodyMembers: MutableList<BodyMemberDeclaration>
) : TypeDeclaration(annotations, modifiers, sortingRule, bodyMembers)

internal class NormalInterfaceDeclaration(
    annotations: MutableList<Annotation>,
    modifiers: MutableList<Modifiers>,
    val identifier: String,
    val typeParameters: MutableList<TypeParameter>,
    val superInterfaces: MutableList<IJvmType>,
    override val sortingRule: Comparator<BodyMemberDeclaration>?,
    override val bodyMembers: MutableList<BodyMemberDeclaration>
) : InterfaceDeclaration(annotations, modifiers, sortingRule, bodyMembers), DocumentedDeclaration {

    constructor(identifier: String):
        this(mutableListOf(), mutableListOf(), identifier, mutableListOf(), mutableListOf(), null, mutableListOf())

    override val documentation = Documentation()

}

internal class ImportDeclaration(
    var container: String,
    var member: String,
    var mode: ImportType? = null,
    var isStatic: Boolean = false,
    var isImplicit: Boolean = false
)

internal class TypeParameter(
    var documentation: String?,
    val annotations: MutableList<Annotation>,
    val identifier: String,
    val bounds: MutableList<IJvmType>,
    var upperBounds: Boolean
) {

    constructor(documentation: String?, identifier: String):
        this(documentation, mutableListOf(), identifier, mutableListOf(), true)

}

internal class EnumConstant(
    val annotations: MutableList<Annotation>,
    val name: String,
    val constructorCall: String?,
    var body: String?
) : DocumentedDeclaration {

    constructor(name: String, constructorCall: String?):
        this(mutableListOf(), name, constructorCall, null)

    override val documentation = Documentation()

}

internal class FieldDeclaration(
    val annotations: MutableList<Annotation>,
    val modifiers: MutableList<Modifiers>,
    val type: IJvmType,
    val entries: MutableMap<String, String?>
) : BodyMemberDeclaration(), DocumentedDeclaration {

    override val documentation = Documentation()

}

internal class FormalParameter(
    val annotations: MutableList<Annotation>,
    val modifiers: MutableList<Modifiers>,
    val type: IJvmType,
    val name: String
)

internal class MethodDeclaration(
    val annotations: MutableList<Annotation>,
    val modifiers: MutableList<Modifiers>,
    val typeParameters: MutableList<TypeParameter>,
    val result: IJvmType,
    val identifier: String,
    val parameters: MutableList<FormalParameter>,
    val exceptions: MutableList<IJvmType>,
    var body: String?
) : BodyMemberDeclaration(), DocumentedDeclaration {

    constructor(result: IJvmType, identifier: String, parameters: MutableList<FormalParameter>):
        this(mutableListOf(), mutableListOf(), mutableListOf(), result, identifier, parameters, mutableListOf(), null)

    override val documentation = Documentation()

}

internal class ConstructorDeclaration(
    val annotations: MutableList<Annotation>,
    val modifiers: MutableList<Modifiers>,
    val typeParameters: MutableList<TypeParameter>,
    val identifier: String,
    val parameters: MutableList<FormalParameter>,
    val exceptions: MutableList<IJvmType>,
    var body: String?
) : BodyMemberDeclaration() {

    constructor(identifier: String, parameters: MutableList<FormalParameter>):
        this(mutableListOf(), mutableListOf(), mutableListOf(), identifier, parameters, mutableListOf(), null)

    val documentation = Documentation()

}

internal class ModularCompilationUnit(
    val name: String,
    val annotations: MutableList<Annotation> = mutableListOf(),
    val modifiers: MutableList<Modifiers> = mutableListOf(),
    override val importDeclarations: MutableMap<String, MutableMap<String, ImportDeclaration>> = mutableMapOf(),
    val sortingRule: Comparator<BodyMemberDeclaration>?,
    val bodyMembers: MutableList<BodyMemberDeclaration> = mutableListOf()
): CompilationUnit() {

    val documentation = Documentation()

    override fun isResolved(type: IJvmType) = isImported(type)

    override fun import(
        container: String,
        member: String,
        mode: ImportType?,
        isStatic: Boolean,
        isImplicit: Boolean
    ) {
        if (mode == null) {
            if (importDeclarations.flatMap { it.value.values }.map { "${it.container}.${it.member}" }.any { it == "$container.$member" || it == "$container.*" }) return
        }

        if (member != IMPORT_WILDCARD && importDeclarations.any { it.value.any { it.key == member } }) return

        val containerImports = importDeclarations.getOrPut(container, ::mutableMapOf)
        if (member in containerImports && mode === null) return

        val factory = { mem: String -> ImportDeclaration(container, mem, mode, isStatic, isImplicit) }

        if (mode === ImportType.QUALIFIED) {
            containerImports[member] = factory.invoke(member)
        } else if (IMPORT_WILDCARD !in containerImports) {
            val filteredImports = containerImports.filter { it.value.mode === null }

            if (filteredImports.size >= 2 || mode === ImportType.WILDCARD) {
                filteredImports.forEach { containerImports.remove(it.key) }
                containerImports[IMPORT_WILDCARD] = factory.invoke(IMPORT_WILDCARD)
            } else
                containerImports[member] = factory.invoke(member)
        }
    }

}

internal class ModuleRequiresDeclaration(
    val module: String
) : BodyMemberDeclaration()

internal class ModuleExportsDeclaration(
    val pack: String,
    val toModules: MutableList<String> = mutableListOf()
) : BodyMemberDeclaration()

internal class ModuleOpensDeclaration(
    val pack: String,
    val toModules: MutableList<String> = mutableListOf()
) : BodyMemberDeclaration()

internal class ModuleUsesDeclaration(
    val service: IJvmType
) : BodyMemberDeclaration()

internal class ModuleProvidesDeclaration(
    val service: IJvmType,
    val impls: List<IJvmType>
) : BodyMemberDeclaration()

internal class Annotation(
    val type: IJvmType,
    val params: String? = null
)

internal class PackageInfo(
    val name: String,
    val annotations: MutableList<Annotation> = mutableListOf(),
    override val importDeclarations: MutableMap<String, MutableMap<String, ImportDeclaration>> = mutableMapOf()
) : CompilationUnit() {

    val documentation = Documentation()

    override fun isResolved(type: IJvmType) = isImported(type)

    override fun import(
        container: String,
        member: String,
        mode: ImportType?,
        isStatic: Boolean,
        isImplicit: Boolean
    ) {
        if (mode == null) {
            if (importDeclarations.flatMap { it.value.values }.map { "${it.container}.${it.member}" }.any { it == "$container.$member" || it == "$container.*" }) return
        }

        if (member != IMPORT_WILDCARD && importDeclarations.any { it.value.any { it.key == member } }) return

        val containerImports = importDeclarations.getOrPut(container, ::mutableMapOf)
        if (member in containerImports && mode === null) return

        val factory = { mem: String -> ImportDeclaration(container, mem, mode, isStatic, isImplicit) }

        if (mode === ImportType.QUALIFIED) {
            containerImports[member] = factory.invoke(member)
        } else if (IMPORT_WILDCARD !in containerImports) {
            val filteredImports = containerImports.filter { it.value.mode === null }

            if (filteredImports.size >= 2 || mode === ImportType.WILDCARD) {
                filteredImports.forEach { containerImports.remove(it.key) }
                containerImports[IMPORT_WILDCARD] = factory.invoke(IMPORT_WILDCARD)
            } else
                containerImports[member] = factory.invoke(member)
        }
    }

}

internal fun List<FormalParameter>.joinAsString(scope: CompilationUnit?) =
    joinToString(", ") { StringBuilder().run {
        if (it.annotations.isNotEmpty()) append(it.annotations.joinAsString(scope)).append(" ")
        if (it.modifiers.isNotEmpty()) append(it.modifiers.toModifierString()).append(" ")
        append(it.type.asString(scope))
        append(" ")
        append(it.name)
        toString()
    }}

internal interface DocumentedDeclaration {

    val documentation: Documentation

}

internal fun MutableList<String>.toQualifiedString() = joinToString(".")