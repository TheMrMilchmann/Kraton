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

import com.github.themrmilchmann.kraton.lang.*
import com.github.themrmilchmann.kraton.lang.java.ast.Annotation
import com.github.themrmilchmann.kraton.lang.java.ast.ConstructorDeclaration
import com.github.themrmilchmann.kraton.lang.java.ast.FormalParameter
import com.github.themrmilchmann.kraton.lang.java.ast.MethodDeclaration
import com.github.themrmilchmann.kraton.lang.java.ast.TypeParameter
import com.github.themrmilchmann.kraton.lang.jvm.*

@KratonDSL
class JavaMethodScope internal constructor(
    internal val declaration: MethodDeclaration
): JavaModifierTarget(), JavaDocumentedScope {

    override val documentation: JavaDocumentationScope
        get() = JavaDocumentationScope(declaration.documentation)

    override fun setModifiers(vararg mods: JavaModifier) {
        mods.forEach {
            when (it) {
                is JavaLanguageModifier -> declaration.modifiers.add(it.mod)
                is Annotate -> declaration.annotations.add(Annotation(it.type, it.params))
                else -> throw IllegalArgumentException("Modifier $it may not be applied to method")
            }
        }
    }

    /**
     * Creates, registers and returns a new [JavaTypeParameterScope] object.
     *
     * @receiver the name for the type-parameter
     *
     * @param doc the documentation for the type-parameter or `null`
     *
     * @return the newly created and registered `JavaTypeParameterScope` object
     *
     * @since 1.0.0
     */
    fun String.typeParam(
        doc: String? = null
    ) = JavaTypeParameterScope(TypeParameter(doc, this))
        .also {
            doc?.apply { this@JavaMethodScope.declaration.documentation.typeParams[it.typeParameter] = this }
            declaration.typeParameters.add(it.typeParameter)
        }

    operator fun String.unaryPlus() {
        declaration.body = if (declaration.body === null)
            this
        else
            declaration.body + "\n" + this
    }

}

@KratonDSL
class JavaConstructorScope internal constructor(
    internal val declaration: ConstructorDeclaration
): JavaModifierTarget(), JavaDocumentedScope {

    override val documentation: JavaDocumentationScope
        get() = JavaDocumentationScope(declaration.documentation)

    override fun setModifiers(vararg mods: JavaModifier) {
        mods.forEach {
            when (it) {
                is JavaLanguageModifier -> declaration.modifiers.add(it.mod)
                is Annotate -> declaration.annotations.add(Annotation(it.type, it.params))
                else -> throw IllegalArgumentException("Modifier $it may not be applied to constructor")
            }
        }
    }

    /**
     * Creates, registers and returns a new [JavaTypeParameterScope] object.
     *
     * @receiver the name for the type-parameter
     *
     * @param doc the documentation for the type-parameter or `null`
     *
     * @return the newly created and registered `JavaTypeParameterScope` object
     *
     * @since 1.0.0
     */
    fun String.typeParam(
        doc: String? = null
    ) = JavaTypeParameterScope(TypeParameter(doc, this))
        .also {
            doc?.apply { this@JavaConstructorScope.declaration.documentation.typeParams[it.typeParameter] = this }
            declaration.typeParameters.add(it.typeParameter)
        }

    operator fun String.unaryPlus() {
        declaration.body = if (declaration.body === null)
            this
        else
            declaration.body + "\n" + this
    }

}

fun IJvmType.param(name: String, doc: String) = JavaParameterScope(FormalParameter(mutableListOf(), mutableListOf(), this, name), doc)

@KratonDSL
class JavaParameterScope internal constructor(
    internal val declaration: FormalParameter,
    internal val doc: String
): JavaModifierTarget() {
    override fun setModifiers(vararg mods: JavaModifier) {
        mods.forEach {
            when (it) {
                is JavaLanguageModifier -> declaration.modifiers.add(it.mod)
                is Annotate -> declaration.annotations.add(Annotation(it.type, it.params))
                else -> throw IllegalArgumentException("Modifier $it may not be applied to parameters")
            }
        }
    }
}