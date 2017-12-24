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

abstract class BodyMemberDeclaration {

    internal abstract fun JavaPrinter.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?)

}

internal open class GroupDeclaration(
    open val sortingRule: Comparator<BodyMemberDeclaration>?,
    open val bodyMembers: MutableList<BodyMemberDeclaration>
) : BodyMemberDeclaration() {

    override fun JavaPrinter.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        if (bodyMembers.isNotEmpty()) {
            bodyMembers.mapIndexed { i, it -> (if (i == 0) null else bodyMembers[i - 1]) to it }
                .mapIndexed { i, it -> it to (if (i ==  bodyMembers.size - 1) null else bodyMembers[i + 1]) }
                .forEach { it.first.second.apply { this@print.print(scope, it.first.first, it.second) } }
        }
    }

}

internal abstract class CompilationUnit

internal class OrdinaryCompilationUnit(
    val packageDeclaration: PackageDeclaration?,
    val importDeclarations: MutableMap<String, MutableMap<String, ImportDeclaration>>,
    val typeDeclaration: TypeDeclaration
) : CompilationUnit() {

    constructor(packageDeclaration: PackageDeclaration, typeDeclaration: TypeDeclaration):
        this(packageDeclaration, mutableMapOf(), typeDeclaration)

    fun isImported(type: IJvmType) =
        importDeclarations[type.packageName]?.any { it.value.member === IMPORT_WILDCARD || it.value.member == type.className } ?: false

    fun isResolved(type: IJvmType) =
        isImported(type) || (packageDeclaration!!.equalsPackage(type))

    fun import(
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

    fun JavaPrinter.print() {
        packageDeclaration?.apply {
            this@print.print()
            println()
        }
        importDeclarations
            .values
            .flatMap { it.values }
            .sortedWith(Comparator { a, b -> "${a.container}.${a.member}".compareTo("${b.container}.${b.member}") })
            .forEach { if (!it.isImplicit) it.apply { this@print.print() } }
        if (importDeclarations.any { it.value.any { !it.value.isImplicit } }) println()
        typeDeclaration.apply { this@print.print(this@OrdinaryCompilationUnit, null, null) }
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

    fun JavaPrinter.print() {
        printI("package ")
        print(identifiers.toQualifiedString())
        println(";")
    }

}

internal abstract class ClassDeclaration(
    annotations: MutableList<Annotation>,
    modifiers: MutableList<Modifiers>,
    sortingRule: Comparator<BodyMemberDeclaration>?,
    bodyMembers: MutableList<BodyMemberDeclaration>
) : TypeDeclaration(annotations, modifiers, sortingRule, bodyMembers)

internal class NormalClassDeclaration(
    annotations: MutableList<Annotation>,
    modifiers: MutableList<Modifiers>,
    val identifier: String,
    val typeParameters: MutableList<TypeParameter>,
    var superClass: IJvmType?,
    val superInterfaces: MutableList<IJvmType>,
    override val sortingRule: Comparator<BodyMemberDeclaration>?,
    override val bodyMembers: MutableList<BodyMemberDeclaration>
) : ClassDeclaration(annotations, modifiers, sortingRule, bodyMembers), DocumentedDeclaration {

    constructor(identifier: String):
        this(mutableListOf(), mutableListOf(), identifier, mutableListOf(), null, mutableListOf(), null, mutableListOf())

    override var documentation = Documentation()

    override fun JavaPrinter.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        documentation.apply { this@print.print(scope) }
        if (annotations.isNotEmpty()) println(annotations.joinAsString(scope, "\n$indent"))
        printI(modifiers.toModifierString())
        if (modifiers.isNotEmpty()) print(" ")
        print("class ")
        print(identifier)
        typeParameters.print(scope)
        superClass?.let {
            print(" extends ")
            print(it.asString(scope))
        }
        if (superInterfaces.isNotEmpty()) {
            print(" implements ")
            print(superInterfaces.joinAsString(scope))
        }
        print(" {")
        if (bodyMembers.isNotEmpty()) {
            println()
            println()
            incIndent()
            bodyMembers.mapIndexed { i, it -> (if (i == 0) null else bodyMembers[i - 1]) to it }
                .mapIndexed { i, it -> it to (if (i ==  bodyMembers.size - 1) null else bodyMembers[i + 1]) }
                .forEach { it.first.second.apply { this@print.print(scope, it.first.first, it.second) } }
            decIndent()
            print(indent)
        }
        print("}")
        if (scope.typeDeclaration !== this@NormalClassDeclaration) println("$ln")
    }

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

    override var documentation = Documentation()

    override fun JavaPrinter.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        documentation.apply { this@print.print(scope) }
        if (annotations.isNotEmpty()) println(annotations.joinAsString(scope, "\n$indent"))
        printI(modifiers.toModifierString())
        if (modifiers.isNotEmpty()) print(" ")
        print("interface ")
        print(identifier)
        typeParameters.print(scope)
        if (superInterfaces.isNotEmpty()) {
            print(" extends ")
            print(superInterfaces.joinAsString(scope))
        }
        print(" {")
        if (bodyMembers.isNotEmpty()) {
            println()
            println()
            incIndent()
            bodyMembers.mapIndexed { i, it -> (if (i == 0) null else bodyMembers[i - 1]) to it }
                .mapIndexed { i, it -> it to (if (i ==  bodyMembers.size - 1) null else bodyMembers[i + 1]) }
                .forEach { it.first.second.apply { this@print.print(scope, it.first.first, it.second) } }
            decIndent()
            print(indent)
        }
        print("}")
        if (scope.typeDeclaration !== this@NormalInterfaceDeclaration) println("$ln")
    }

}

internal class FieldDeclaration(
    val annotations: MutableList<Annotation>,
    val modifiers: MutableList<Modifiers>,
    val type: IJvmType,
    val entries: MutableMap<String, String?>
) : BodyMemberDeclaration(), DocumentedDeclaration {

    override var documentation = Documentation()

    override fun JavaPrinter.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        documentation.apply { this@print.print(scope) }
        if (annotations.isNotEmpty()) println(annotations.joinAsString(scope, "\n$indent"))
        printI(modifiers.toModifierString())
        if (modifiers.isNotEmpty()) print(" ")
        print(type.asString(scope))
        print(" ")

        if (entries.size > 1) {
            // TODO impl
        } else {
            print(entries.entries.first().key)
            entries.entries.first().value?.let { print(" = $it") }
        }
        println(";")
        if (next === null) println()
    }

}

internal class ImportDeclaration(
    var container: String,
    var member: String,
    var mode: ImportType? = null,
    var isStatic: Boolean = false,
    var isImplicit: Boolean = false
) {

    fun JavaPrinter.print() {
        printI("import ")
        if (isStatic) print("static ")
        println("$container.$member;")
    }

}

internal class TypeParameter(
    var documentation: String?,
    val annotations: MutableList<IJvmType>,
    val identifier: String,
    val bounds: MutableList<IJvmType>,
    var upperBounds: Boolean
) {

    constructor(documentation: String?, identifier: String):
        this(documentation, mutableListOf(), identifier, mutableListOf(), true)

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
    private val result: IJvmType,
    private val identifier: String,
    private val parameters: MutableList<FormalParameter>,
    val exceptions: MutableList<IJvmType>,
    var body: String?
) : BodyMemberDeclaration(), DocumentedDeclaration {

    constructor(result: IJvmType, identifier: String, parameters: MutableList<FormalParameter>):
        this(mutableListOf(), mutableListOf(), mutableListOf(), result, identifier, parameters, mutableListOf(), null)

    override val documentation = Documentation()

    override fun JavaPrinter.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        if (prev is FieldDeclaration) println()

        documentation.apply { this@print.print(scope) }
        if (annotations.isNotEmpty()) println(annotations.joinAsString(scope, "\n$indent"))
        printI(modifiers.toModifierString())
        if (modifiers.isNotEmpty()) print(" ")
        typeParameters.print(scope)
        if (typeParameters.isNotEmpty()) print(" ")
        print(result.asString(scope))
        print(" ")
        print(identifier)
        print("(")
        print(parameters.joinAsString(scope))
        print(")")
        if (exceptions.isNotEmpty()) {
            print(" throws ")
            print(exceptions.joinAsString(scope))
        }
        body?.let {
            print(" {")
            if (it.isNotEmpty()) {
                printMethodBody(it)
                print(indent)
            }
            println("}")
        } ?: println(";")
        println()
    }

}

internal class ConstructorDeclaration(
    val annotations: MutableList<Annotation>,
    val modifiers: MutableList<Modifiers>,
    val typeParameters: MutableList<TypeParameter>,
    private val identifier: String,
    private val parameters: MutableList<FormalParameter>,
    val exceptions: MutableList<IJvmType>,
    var body: String?
) : BodyMemberDeclaration() {

    constructor(identifier: String, parameters: MutableList<FormalParameter>):
        this(mutableListOf(), mutableListOf(), mutableListOf(), identifier, parameters, mutableListOf(), null)

    val documentation = Documentation()

    override fun JavaPrinter.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        if (prev is FieldDeclaration) println()

        documentation.apply { this@print.print(scope) }
        if (annotations.isNotEmpty()) println(annotations.joinAsString(scope, "\n$indent"))
        printI(modifiers.toModifierString())
        if (modifiers.isNotEmpty()) print(" ")
        typeParameters.print(scope)
        if (typeParameters.isNotEmpty()) print(" ")
        print(identifier)
        print("(")
        print(parameters.joinAsString(scope))
        print(")")
        if (exceptions.isNotEmpty()) {
            print(" throws ")
            print(exceptions.joinAsString(scope))
        }
        body?.let {
            print(" {")
            if (it.isNotEmpty()) {
                printMethodBody(it)
                print(indent)
            }
            println("}")
        } ?: println(";")
        println()
    }

}

internal class Annotation(
    val type: IJvmType,
    val params: String? = null
)

internal fun List<FormalParameter>.joinAsString(scope: OrdinaryCompilationUnit?) =
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