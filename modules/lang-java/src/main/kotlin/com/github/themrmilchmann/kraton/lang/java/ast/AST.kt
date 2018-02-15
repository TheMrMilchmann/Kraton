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
package com.github.themrmilchmann.kraton.lang.java.ast

import com.github.themrmilchmann.kraton.lang.java.impl.*
import com.github.themrmilchmann.kraton.lang.java.impl.model.*
import com.github.themrmilchmann.kraton.lang.jvm.*
import java.util.*

const val IMPORT_WILDCARD = "*"

sealed class BodyMemberDeclaration

internal sealed class GroupDeclaration : BodyMemberDeclaration() {
    abstract val sortingRule: Comparator<BodyMemberDeclaration>?
    abstract val bodyMembers: MutableList<BodyMemberDeclaration>
}

internal data class VirtualGroupDeclaration(
    override val sortingRule: Comparator<BodyMemberDeclaration>?,
    override val bodyMembers: MutableList<BodyMemberDeclaration> = mutableListOf()
) : GroupDeclaration()

internal sealed class CompilationUnit {
    abstract val importDeclarations: MutableMap<String, MutableMap<String, ImportDeclaration>>
}

internal data class OrdinaryCompilationUnit(
    val packageDeclaration: PackageDeclaration?,
    override val importDeclarations: MutableMap<String, MutableMap<String, ImportDeclaration>>,
    val typeDeclaration: TypeDeclaration
) : CompilationUnit() {

    init {
        importDeclarations.put("java.lang", mutableMapOf("*" to ImportDeclaration("java.lang", "*", isImplicit = true)))
    }

    constructor(packageDeclaration: PackageDeclaration, typeDeclaration: TypeDeclaration):
        this(packageDeclaration, mutableMapOf(), typeDeclaration)

}

internal sealed class TypeDeclaration : GroupDeclaration(), DocumentedDeclaration {
    abstract val annotations: MutableList<Annotation>
    abstract val modifiers: MutableList<Modifiers>
}

internal data class PackageDeclaration(
    val identifiers: MutableList<String>
) {

    fun equalsPackage(type: IJvmType) =
        identifiers.toQualifiedString() == type.packageName

}

internal sealed class ClassDeclaration : TypeDeclaration() {
    abstract val superInterfaces: MutableList<IJvmType>
}

internal data class NormalClassDeclaration(
    override val annotations: MutableList<Annotation>,
    override val modifiers: MutableList<Modifiers>,
    val identifier: String,
    val typeParameters: MutableList<TypeParameter>,
    var superClass: IJvmType?,
    override val superInterfaces: MutableList<IJvmType>,
    override val sortingRule: Comparator<BodyMemberDeclaration>?,
    override val bodyMembers: MutableList<BodyMemberDeclaration>
) : ClassDeclaration(), DocumentedDeclaration {

    constructor(identifier: String):
        this(mutableListOf(), mutableListOf(), identifier, mutableListOf(), null, mutableListOf(), null, mutableListOf())

    override val documentation = Documentation()

}

internal data class EnumClassDeclaration(
    override val annotations: MutableList<Annotation>,
    override val modifiers: MutableList<Modifiers>,
    val identifier: String,
    override val superInterfaces: MutableList<IJvmType>,
    val values: MutableList<EnumConstant>,
    override val sortingRule: Comparator<BodyMemberDeclaration>?,
    override val bodyMembers: MutableList<BodyMemberDeclaration>
) : ClassDeclaration() {

    constructor(identifier: String):
        this(mutableListOf(), mutableListOf(), identifier, mutableListOf(), mutableListOf(), null, mutableListOf())

    override val documentation = Documentation()

}

internal sealed class InterfaceDeclaration : TypeDeclaration()

internal data class NormalInterfaceDeclaration(
    override val annotations: MutableList<Annotation>,
    override val modifiers: MutableList<Modifiers>,
    val identifier: String,
    val typeParameters: MutableList<TypeParameter>,
    val superInterfaces: MutableList<IJvmType>,
    override val sortingRule: Comparator<BodyMemberDeclaration>?,
    override val bodyMembers: MutableList<BodyMemberDeclaration>
) : InterfaceDeclaration(), DocumentedDeclaration {

    constructor(identifier: String):
        this(mutableListOf(), mutableListOf(), identifier, mutableListOf(), mutableListOf(), null, mutableListOf())

    override val documentation = Documentation()

}

internal data class ImportDeclaration(
    var container: String,
    var member: String,
    var mode: ImportType? = null,
    var isStatic: Boolean = false,
    var isImplicit: Boolean = false
)

internal data class TypeParameter(
    var documentation: String?,
    val annotations: MutableList<Annotation>,
    val identifier: String,
    val bounds: MutableList<IJvmType>,
    var upperBounds: Boolean
) {

    constructor(documentation: String?, identifier: String):
        this(documentation, mutableListOf(), identifier, mutableListOf(), true)

}

internal data class EnumConstant(
    val annotations: MutableList<Annotation>,
    val name: String,
    val constructorCall: String?,
    var body: String?
) : DocumentedDeclaration {

    constructor(name: String, constructorCall: String?):
        this(mutableListOf(), name, constructorCall, null)

    override val documentation = Documentation()

}

internal data class Initializer(
    val isStatic: Boolean,
    var body: String = ""
) : BodyMemberDeclaration()

internal data class FieldDeclaration(
    val annotations: MutableList<Annotation>,
    val modifiers: MutableList<Modifiers>,
    val type: IJvmType,
    val entries: MutableMap<String, String?>
) : BodyMemberDeclaration(), DocumentedDeclaration {

    override val documentation = Documentation()

}

internal data class FormalParameter(
    val annotations: MutableList<Annotation>,
    val modifiers: MutableList<Modifiers>,
    val type: IJvmType,
    val name: String
)

internal data class MethodDeclaration(
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

internal data class ConstructorDeclaration(
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

internal data class ModularCompilationUnit(
    val name: String,
    val annotations: MutableList<Annotation> = mutableListOf(),
    val modifiers: MutableList<Modifiers> = mutableListOf(),
    override val importDeclarations: MutableMap<String, MutableMap<String, ImportDeclaration>> = mutableMapOf(),
    val sortingRule: Comparator<BodyMemberDeclaration>?,
    val bodyMembers: MutableList<BodyMemberDeclaration> = mutableListOf()
): CompilationUnit() {

    val documentation = Documentation()

}

internal data class ModuleRequiresDeclaration(
    val module: String
) : BodyMemberDeclaration()

internal data class ModuleExportsDeclaration(
    val pack: String,
    val toModules: MutableList<String> = mutableListOf()
) : BodyMemberDeclaration()

internal data class ModuleOpensDeclaration(
    val pack: String,
    val toModules: MutableList<String> = mutableListOf()
) : BodyMemberDeclaration()

internal data class ModuleUsesDeclaration(
    val service: IJvmType
) : BodyMemberDeclaration()

internal data class ModuleProvidesDeclaration(
    val service: IJvmType,
    val impls: List<IJvmType>
) : BodyMemberDeclaration()

internal data class Annotation(
    val type: IJvmType,
    val params: String? = null
)

internal data class PackageInfo(
    val name: String,
    val annotations: MutableList<Annotation> = mutableListOf(),
    override val importDeclarations: MutableMap<String, MutableMap<String, ImportDeclaration>> = mutableMapOf()
) : CompilationUnit() {

    val documentation = Documentation()

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

internal fun MutableList<String>.toQualifiedString() = joinToString("")