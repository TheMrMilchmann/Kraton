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
 * Creates, registers and returns a new [JavaInterfaceScope] object.
 *
 * Unless a [sortingRule] is specified, all members are generated in the order
 * in which they have been registered.
 *
 * @receiver the template file that serves as wrapper for this interface
 *
 * @param name          the name for the interface
 * @param packageName   the package the interface is to be placed in
 * @param outputDir     the output directory for the interface (Note that this
 *                      should not be the absolute directory, but relative from
 *                      the `output-root` directory that is given to the
 *                      generator.)
 * @param sortingRule   the `Comparator` that will be used to sort this scope's
 *                      members
 * @param init          initialize the interface
 *
 * @return the newly created and registered `JavaInterfaceScope` object
 *
 * @since 1.0.0
 */
fun TemplateFile.javaInterface(
    name: String,
    packageName: String,
    outputDir: String,
    sortingRule: Comparator<BodyMemberDeclaration>? = null,
    init: JavaInterfaceScope.() -> Unit
) : JavaInterfaceScope {
    val packageDeclaration = PackageDeclaration(mutableListOf(packageName))
    val normalInterfaceDeclaration = NormalInterfaceDeclaration(name)
    val ordinaryCompilationUnit = OrdinaryCompilationUnit(packageDeclaration, normalInterfaceDeclaration)

    return JavaInterfaceScope(ordinaryCompilationUnit, normalInterfaceDeclaration, sortingRule)
        .also(init)
        .also { Template(JAVA_ADAPTER, outputDir, "$packageName/$name.java", { beginOrdinaryCompilationUnit(ordinaryCompilationUnit) }).reg() }
}

/**
 * Creates, registers and returns a new [JavaInterfaceScope] object.
 *
 * Unless a [sortingRule] is specified, all members are generated in the order
 * in which they have been registered.
 *
 * @receiver the enclosing type for the interface
 *
 * @param name          the name for the interface
 * @param sortingRule   the `Comparator` that will be used to sort this scope's
 *                      members
 * @param init          initialize the interface
 *
 * @return the newly created and registered `JavaInterfaceScope` object
 *
 * @since 1.0.0
 */
fun JavaOrdinaryCompilationUnitScope<*>.javaInterface(
    name: String,
    sortingRule: Comparator<BodyMemberDeclaration>? = null,
    init: JavaInterfaceScope.() -> Unit
) : JavaInterfaceScope {
    val normalInterfaceDeclaration = NormalInterfaceDeclaration(name)

    return JavaInterfaceScope(compilationUnit, normalInterfaceDeclaration, sortingRule)
        .also(init)
        .also { bodyMembers.add(normalInterfaceDeclaration) }
}

class JavaInterfaceScope internal constructor(
    compilationUnit: OrdinaryCompilationUnit,
    override val declaration: NormalInterfaceDeclaration,
    private val sortingRule: Comparator<BodyMemberDeclaration>?,
    bodyMembers: MutableList<BodyMemberDeclaration> = declaration.bodyMembers
) : JavaOrdinaryCompilationUnitScope<JavaInterfaceScope>(compilationUnit, declaration, bodyMembers) {

    override val className get() = declaration.identifier

    /**
     * TODO doc
     *
     * @param types the supertypes for this interface
     *
     * @since 1.0.0
     */
    fun extends(vararg types: IJvmType) {
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
            doc?.apply { this@JavaInterfaceScope.declaration.documentation.typeParams[it.typeParameter] = this }
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
                declaration.documentation.params[it.declaration] = it.doc
            }
            exceptions?.forEach {
                import(it.first)
                declaration.exceptions.add(it.first)
                it.second?.apply { declaration.documentation.exceptions[it.first] = this }
            }
        }

    /**
     * Creates, registers and returns a new [JavaInterfaceScope] object.
     *
     * A scope created by this method serves as a logical group to structure the
     * members of the scope it was created in.
     *
     * Unless a [sortingRule] is specified, all members are generated in the
     * order in which they have been registered.
     *
     * If a sorting rule has been specified for the current scope, this function
     * is a no-op and may be used to structure template sources only.
     *
     * @param sortingRule   the `Comparator` that will be used to sort this
     *                      scope's members
     * @param init          initialize the group
     *
     * @return
     *
     * @since 1.0.0
     */
    override fun group(sortingRule: Comparator<BodyMemberDeclaration>?, init: JavaInterfaceScope.() -> Unit): JavaInterfaceScope =
        this.sortingRule?.let {
            GroupDeclaration(sortingRule).let {
                bodyMembers.add(it)

                JavaInterfaceScope(compilationUnit, declaration, sortingRule, it.bodyMembers).also(init)
            }
        } ?: apply(init)

}