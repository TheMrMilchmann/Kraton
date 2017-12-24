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
import com.github.themrmilchmann.kraton.lang.java.impl.*
import com.github.themrmilchmann.kraton.lang.jvm.*

/**
 * Creates, registers and returns a new [JavaClassScope] object.
 *
 * @receiver the template file that serves as wrapper for this class
 *
 * @param name          the name for the class
 * @param packageName   the package the class is to be placed in
 * @param init          initialize the class
 *
 * @return the newly created and registered `JavaClassScope` object
 *
 * @since 1.0.0
 */
fun TemplateFile.javaClass(
    name: String,
    packageName: String,
    outputSourceSet: String,
    sortingRule: Comparator<BodyMemberDeclaration>? = null,
    init: JavaClassScope.() -> Unit
) : JavaClassScope {
    val packageDeclaration = PackageDeclaration(mutableListOf(packageName))
    val normalClassDeclaration = NormalClassDeclaration(name)
    val ordinaryCompilationUnit = OrdinaryCompilationUnit(packageDeclaration, normalClassDeclaration)

    return JavaClassScope(ordinaryCompilationUnit, normalClassDeclaration, sortingRule)
        .also(init)
        .also { Template(JAVA_ADAPTER, outputSourceSet, "$packageName/$name.java", { beginOrdinaryCompilationUnit(ordinaryCompilationUnit) }).reg() }
}

/**
 * Creates, registers and returns a new [JavaClassScope] object.
 *
 * @receiver the enclosing type for the class
 *
 * @param name the name for the class
 * @param init initialize the class
 *
 * @return the newly created and registered `JavaClassScope` object
 *
 * @since 1.0.0
 */
fun JavaOrdinaryCompilationUnitScope<*>.javaClass(
    name: String,
    sortingRule: Comparator<BodyMemberDeclaration>? = null,
    init: JavaClassScope.() -> Unit
) : JavaClassScope {
    val normalClassDeclaration = NormalClassDeclaration(name)

    return JavaClassScope(compilationUnit, normalClassDeclaration, sortingRule)
        .also(init)
        .also { bodyMembers.add(normalClassDeclaration) }
}

/**
 * TODO doc
 *
 * @param compilationUnit
 * @param declaration
 *
 * @since 1.0.0
 */
class JavaClassScope internal constructor(
    compilationUnit: OrdinaryCompilationUnit,
    override val declaration: NormalClassDeclaration,
    private val sortingRule: Comparator<BodyMemberDeclaration>?,
    bodyMembers: MutableList<BodyMemberDeclaration> = declaration.bodyMembers
) : JavaOrdinaryCompilationUnitScope<JavaClassScope>(compilationUnit, declaration, bodyMembers) {

    override val className get() = declaration.identifier

    /**
     * TODO doc
     *
     * Each time this function is called the previously stored supertype is
     * overwritten.
     *
     * @param type the supertype for this class
     *
     * @since 1.0.0
     */
    fun extends(type: IJvmType?) {
        if (type != null) import(type)
        declaration.superClass = type
    }

    /**
     * TODO doc
     *
     * @param types
     *
     * @since 1.0.0
     */
    fun implements(vararg types: IJvmType) {
        types.forEach {
            import(it)
            declaration.superInterfaces.add(it)
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
            doc?.apply { this@JavaClassScope.declaration.documentation.typeParams[it.typeParameter] = this }
            declaration.typeParameters.add(it.typeParameter)
        }

    /**
     * Creates, registers and returns a new [JavaFieldScope] object.
     *
     * A `JavaFieldScope` may represent one or multiple fields that of the same
     * type.
     *
     * @receiver the type for the field/s
     *
     * @param init TODO doc
     *
     * @return the newly created and registered `JavaFieldScope` object
     *
     * @since 1.0.0
     */
    operator fun IJvmType.invoke(
        init: JavaFieldScope.() -> Unit
    ) = JavaFieldScope(FieldDeclaration(mutableListOf(), mutableListOf(), this, mutableMapOf()).also { bodyMembers.add(it) })
        .apply(init)
        .also { import(this) }

    /**
     * Creates, registers and returns a new [JavaConstructorScope] object.
     *
     * Modifiers may be prepended as described in the documentation of
     * [JavaModifierTarget]. The generator does not attempt to validate
     * modifiers in any way.
     *
     * Javadoc may be added by using the [JavaDocumentationScope] modifier.
     *
     * @param parameters    the parameters for the method
     * @param exceptions    the exception (paired with their documentation or
     *                      `null` for no documentation) that may be thrown by
     *                      the method
     * @param init          initialize the method with additional information
     *                      (type parameters, a method body, etc.)
     *
     * @return the newly created and registered `JavaConstructorScope` object.
     *
     * @since 1.0.0
     */
    @JvmOverloads
    fun constructor(
        vararg parameters: JavaParameterScope,
        exceptions: List<Pair<IJvmType, String?>>? = null,
        init: (JavaConstructorScope.() -> Unit)? = null
    ) = JavaConstructorScope(ConstructorDeclaration(declaration.identifier, parameters.mapTo(mutableListOf()) { it.declaration }).also { bodyMembers.add(it) })
        .apply {
            init?.invoke(this)
            parameters.forEach {
                import(it.declaration.type)
                declaration.documentation.params[it.declaration] = it.doc
            }
            exceptions?.forEach {
                import(it.first)
                declaration.exceptions.add(it.first)
                it.second?.apply {documentation.declaration.exceptions[it.first] = this }
            }
        }

    /**
     * Creates, registers and returns a new [JavaMethodScope] object.
     *
     * Modifiers may be prepended as described in the documentation of
     * [JavaModifierTarget]. The generator does not attempt to validate
     * modifiers in any way.
     *
     * Javadoc may be added by using the [JavaDocumentationScope] modifier.
     *
     * @receiver the return type for the method
     *
     * @param name          the name for the method
     * @param parameters    the parameters for the method
     * @param exceptions    the exception (paired with their documentation or
     *                      `null` for no documentation) that may be thrown by
     *                      the method
     * @param init          initialize the method with additional information
     *                      (type parameters, a method body, etc.)
     *
     * @return the newly created and registered `JavaMethodScope` object.
     *
     * @since 1.0.0
     */
    operator fun IJvmType.invoke(
        name: String,
        vararg parameters: JavaParameterScope,
        exceptions: List<Pair<IJvmType, String?>>? = null,
        init: (JavaMethodScope.() -> Unit)? = null
    ) = JavaMethodScope(MethodDeclaration(this, name, parameters.mapTo(mutableListOf()) { it.declaration }).also { bodyMembers.add(it) })
        .apply {
            init?.invoke(this)
            import(this@invoke)
            parameters.forEach {
                import(it.declaration.type)
                documentation.declaration.params[it.declaration] = it.doc
            }
            exceptions?.forEach {
                import(it.first)
                declaration.exceptions.add(it.first)
                it.second?.apply {documentation.declaration.exceptions[it.first] = this }
            }
        }

    /**
     * TODO doc
     *
     * @return
     *
     * @since 1.0.0
     */
    override fun group(sortingRule: Comparator<BodyMemberDeclaration>?, init: JavaClassScope.() -> Unit): JavaClassScope {
        return if (this.sortingRule == null) {
            init.invoke(this)

            this
        } else {
            val groupDeclaration = GroupDeclaration(sortingRule, mutableListOf())

            JavaClassScope(compilationUnit, declaration, sortingRule, groupDeclaration.bodyMembers)
                .also(init)
                .also { bodyMembers.add(groupDeclaration) }
        }
    }

}