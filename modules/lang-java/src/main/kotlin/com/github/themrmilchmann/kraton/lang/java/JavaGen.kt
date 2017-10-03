/*
 * Copyright (c) 2017 Leon Linhart,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.themrmilchmann.kraton.lang.java

import com.github.themrmilchmann.kraton.*
import com.github.themrmilchmann.kraton.lang.jvm.*
import java.io.*
import java.util.*
import kotlin.collections.LinkedHashSet

internal const val INDENT = "    "
internal const val LN = "\n"
internal const val IMPORT_WILDCARD = "*"

class JavaBodyMemberGroup<T: JavaTopLevelType<T, S>, S: JavaScope<T, S>> internal constructor(
    override val name: String = "",
    sortingStrategy: Comparator<JavaBodyMember>? = null,
    private val headerPrinter: (PrintWriter.(indent: String, group: JavaBodyMemberGroup<*, *>) -> Unit)? = null
): JavaBodyMember {

    internal constructor(
        containerType: T,
        name: String,
        sortingStrategy: Comparator<JavaBodyMember>?,
        headerPrinter: (PrintWriter.(indent: String, group: JavaBodyMemberGroup<*, *>) -> Unit)?,
        init: S.() -> Unit
    ): this(name, sortingStrategy, headerPrinter) {
        containerType.scope(members, init)
    }

    internal val members: MutableSet<JavaBodyMember> = if (sortingStrategy != null) TreeSet(sortingStrategy) else LinkedHashSet()

    override fun PrintWriter.printMember(indent: String, containerType: JavaTopLevelType<*, *>) {
        headerPrinter?.invoke(this, indent, this@JavaBodyMemberGroup)

        val subIndent = indent + INDENT
        var wasField = false

        members.forEach {
            val isField = it is JavaField
            if ((wasField && !isField)) println()
            wasField = isField

            it.run { printMember(subIndent, containerType) }
        }

        if (wasField) println()
    }

}

internal fun <T: JavaTopLevelType<T, *>> Profile.targetOf(type: T, packageName: String, srcFolder: String, srcSet: String, copyrightHeader: String?) =
    JavaGeneratorTarget(type.className, packageName, srcFolder, srcSet, {
        if (copyrightHeader != null) println(copyrightHeader)
        print("package ")
        print(packageName)
        println(";")
        println()

        val imports = type.imports.flatMap { it.value.values }.filter { !it.isImplicit }

        if (imports.any()) {
            imports.sorted().forEach { it.apply { printImport() } }
            println()
        }

        type.apply { printType("") }
    }).run {
        targets.add(this)
        type
    }

internal class JavaGeneratorTarget(
    fileName: String,
    packageName: String,
    srcFolder: String,
    srcSet: String,
    private val print: PrintWriter.() -> Unit
): GeneratorTarget(fileName, "java", packageName, srcFolder, srcSet) {

    override fun PrintWriter.printTarget() {
        this.apply(print)
    }

}

/**
 * A java top-level type.
 *
 * @property className      the name of this type
 * @property packageName    the name of the package containing this type
 * @property documentation  the documentation of this type
 *
 * @constructor Creates a new java top-level type.
 *
 * @since 1.0.0
 */
abstract class JavaTopLevelType<T: JavaTopLevelType<T, S>, S: JavaScope<T, S>>(
    override val className: String,
    override val packageName: String,
    val documentation: String?,
    val since: String?,
    private val containerType: JavaTopLevelType<*, *>?
): JavaModifierTarget(), JavaBodyMember, IJvmType {

    override val enclosingType get() = containerType

    private val _imports by lazy { mutableMapOf<String, MutableMap<String, JavaImport>>() }
    internal val imports: MutableMap<String, MutableMap<String, JavaImport>> get() = containerType?.imports ?: _imports

    private val rootGroup = JavaBodyMemberGroup<T, S>()
    internal val members get() = rootGroup.members

    internal val typeParameters = mutableListOf<Pair<JvmGenericType, String?>>()

    private val _authors: MutableList<String> by lazy(::mutableListOf)
    private val authors: MutableList<String> get() = containerType?.authors ?: _authors
    fun authors(vararg authors: String) { this.authors.addAll(authors) }

    private val references = mutableListOf<String>()
    fun see(ref: String) { this.references.add(ref) }

    private fun doImport(
        container: String,
        member: String,
        forceMode: JavaImportForceMode?,
        isStatic: Boolean,
        isImplicit: Boolean
    ) {
        if (forceMode == null) {
            if (container == packageName) return
            if (imports.flatMap { it.value.values }.map { "${it.container}.${it.member}" }.any { it == "$container.$member" || it == "$container.*" }) return
        }

        if (member != IMPORT_WILDCARD && imports.any { it.value.any { it.key == member } }) return

        val containerImports = imports.getOrPut(container, ::mutableMapOf)
        if (member in containerImports && forceMode === null) return

        val factory = { mem: String -> JavaImport(container, mem, forceMode, isStatic, isImplicit) }

        if (forceMode === JavaImportForceMode.FORCE_QUALIFIED) {
            containerImports[member] = factory.invoke(member)
        } else if (IMPORT_WILDCARD !in containerImports) {
            val filteredImports = containerImports.filter { it.value.forceMode === null }

            if (filteredImports.size >= 2 || forceMode === JavaImportForceMode.FORCE_WILDCARD) {
                filteredImports.forEach { containerImports.remove(it.key) }
                containerImports[IMPORT_WILDCARD] = factory.invoke(IMPORT_WILDCARD)
            } else
                containerImports[member] = factory.invoke(member)
        }
    }

    internal fun import(type: String, isStatic: Boolean = false, isImplicit: Boolean = false) =
        doImport(type, IMPORT_WILDCARD, JavaImportForceMode.FORCE_WILDCARD, isStatic, isImplicit)

    internal fun import(
        type: IJvmType,
        forceMode: JavaImportForceMode? = null,
        isImplicit: Boolean = false
    ) = type.packageName?.let { doImport(it, type.containerName, forceMode, false, isImplicit) }

    internal fun import(
        type: IJvmType,
        member: String,
        forceMode: JavaImportForceMode? = null,
        isImplicit: Boolean = false
    ) = type.packageName?.let { doImport("$it.$type", member, forceMode, true, isImplicit) }

    fun isImported(type: IJvmType) =
        imports[type.packageName]?.any { it.value.member === IMPORT_WILDCARD || it.value.member == type.className } ?: false

    fun isResolved(type: IJvmType) =
        isImported(type) || type.packageName == packageName

    internal fun PrintWriter.printType(indent: String) {
        val documentation = documentation.toJavaDoc(indent, typeParameters, references, authors, since)
        if (documentation != null) println(documentation)

        print(indent)
        printAnnotations(indent, this@JavaTopLevelType)
        printModifiers()
        printTypeDeclaration()
        print(" {")

        if (rootGroup.members.isNotEmpty()) {
            println(LN)
            rootGroup.apply { printMember(indent, this@JavaTopLevelType) }
            print(indent)
        }

        print("}")
    }

    abstract fun scope(members: MutableSet<JavaBodyMember>, init: S.() -> Unit): S

    /**
     * Prints the declaration of this top-level type.
     *
     * @receiver the PrintWriter to print to
     *
     * @since 1.0.0
     */
    internal abstract fun PrintWriter.printTypeDeclaration()

    override fun nullable() = JvmTypeReference(className, packageName, nullable = true)

    override fun PrintWriter.printMember(indent: String, containerType: JavaTopLevelType<*, *>) {
        printType(indent)
        println(LN)
    }

}

interface JavaBodyMember : Comparable<JavaBodyMember> {

    /**
     * The name of this java object.
     *
     * @since 1.0.0
     */
    val name: String

    override fun compareTo(other: JavaBodyMember) = name.compareTo(other.name)

    /**
     * Prints this java object to the given PrintWriter.
     *
     * @receiver the receiver to which this member will be printed
     *
     * @param indent        the indent to be used
     * @param containerType the type this member will be printed in
     *
     * @since 1.0.0
     */
    fun PrintWriter.printMember(indent: String, containerType: JavaTopLevelType<*, *>)

}

enum class JavaImportForceMode {
    FORCE_QUALIFIED,
    FORCE_WILDCARD
}

internal class JavaImport(
    val container: String,
    val member: String,
    val forceMode: JavaImportForceMode?,
    val isStatic: Boolean,
    val isImplicit: Boolean
): Comparable<JavaImport> {

    override fun compareTo(other: JavaImport): Int {
        if (isStatic) {
            if (!other.isStatic)
                return 1
        } else if (other.isStatic)
            return -1

        val cmp = container.compareTo(other.container)
        if (cmp != 0) return cmp

        if (member == "*" || other.member == "*") return 0

        return member.compareTo(other.member)
    }

    fun PrintWriter.printImport() = println(this@JavaImport)

    override fun toString() = "import ${if (isStatic) "static " else ""}$container.$member;"

}