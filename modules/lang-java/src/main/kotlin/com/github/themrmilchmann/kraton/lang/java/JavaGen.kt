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
package com.github.themrmilchmann.kraton.lang.java

import com.github.themrmilchmann.kraton.lang.java.impl.*
import com.github.themrmilchmann.kraton.lang.java.impl.model.*
import com.github.themrmilchmann.kraton.lang.jvm.*

abstract class JavaOrdinaryCompilationUnitScope<S : JavaOrdinaryCompilationUnitScope<S>> internal constructor(
    internal val compilationUnit: OrdinaryCompilationUnit,
    internal open val declaration: TypeDeclaration,
    internal val bodyMembers: MutableList<BodyMemberDeclaration>
) : JavaModifierTarget(), JavaDocumentedScope, IJvmType {

    override val nullable by lazy { JvmTypeReference(className, packageName, nullable = true) }

    override fun setModifiers(vararg mods: JavaModifier) {
        mods.forEach {
            when (it) {
                is JavaLanguageModifier -> declaration.modifiers.add(it.mod)
                is Annotate -> declaration.annotations.add(Annotation(it.type, it.params))
                else -> throw IllegalArgumentException("Modifier $it may not be applied to top-level type")
            }
        }
    }

    /**
     * TODO doc
     *
     * @since 1.0.0
     */
    override val documentation: JavaDocumentationScope
        get() = JavaDocumentationScope(declaration.documentation)

    init {
        /* Always import `java.lang.*` implicitly. It might be a good idea to move this to the AST. */
        import("java.lang", isImplicit = true)
    }

    /**
     * TODO doc
     *
     * @param init
     *
     * @since 1.0.0
     */
    fun documentation(init: JavaDocumentationScope.() -> Unit) {
        documentation.also(init)
    }

    /**
     * TODO doc
     *
     * @since 1.0.0
     */
    fun isImported(type: IJvmType) = compilationUnit.isImported(type)

    /**
     * Returns whether or not the given `type` is resolved for this compilation
     * unit, that is, the type may be referenced by it's simple name.
     *
     * @param type the type to look up
     *
     * @return whether or not the given `type` is resolved
     *
     * @since 1.0.0
     */
    fun isResolved(type: IJvmType) = compilationUnit.isResolved(type)

    /**
     * Returns the name for the given type as seen from the current compilation
     * unit.
     *
     * The returned name may be used in documentation or method bodies.
     *
     * @return the name for the given type as seen from the current compilation
     *         unit
     *
     * @since 1.0.0
     */
    val IJvmType.resolvedName: String get() = asString(compilationUnit)

    fun import(type: String, isStatic: Boolean = false, isImplicit: Boolean = false) =
        compilationUnit.import(type, IMPORT_WILDCARD, ImportType.WILDCARD, isStatic, isImplicit)

    fun import(
        type: IJvmType,
        forceMode: ImportType? = null,
        isImplicit: Boolean = false
    ) = type.packageName?.let { compilationUnit.import(it, type.containerName, forceMode, false, isImplicit) }

    fun import(
        type: IJvmType,
        member: String,
        forceMode: ImportType? = null,
        isImplicit: Boolean = false
    ) = type.packageName?.let { compilationUnit.import("$it.$type", member, forceMode, true, isImplicit) }

    abstract fun group(sortingRule: Comparator<BodyMemberDeclaration>? = null, init: S.() -> Unit) : S

    infix fun JavaTypeParameterScope.extends(type: IJvmType) {
        if (typeParameter.bounds.isNotEmpty() && !typeParameter.upperBounds)
            throw IllegalStateException()

        import(type)
        typeParameter.bounds.add(type)
        typeParameter.upperBounds = true
    }

    infix fun JavaTypeParameterScope.extends(types: Array<IJvmType>) {
        if (typeParameter.bounds.isNotEmpty() && !typeParameter.upperBounds)
            throw IllegalStateException()

        types.forEach { import(it) }
        typeParameter.bounds.addAll(types)
        typeParameter.upperBounds = true
    }

    infix fun JavaTypeParameterScope.`super`(type: IJvmType) {
        if (typeParameter.bounds.isNotEmpty() && typeParameter.upperBounds)
            throw IllegalStateException()

        import(type)
        typeParameter.bounds.add(type)
        typeParameter.upperBounds = false
    }

    infix fun JavaTypeParameterScope.`super`(types: Array<IJvmType>) {
        if (typeParameter.bounds.isNotEmpty() && typeParameter.upperBounds)
            throw IllegalStateException()

        types.forEach { import(it) }
        typeParameter.bounds.addAll(types)
        typeParameter.upperBounds = false
    }

}