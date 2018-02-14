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

fun TemplateFile.javaModuleInfo(
    name: String,
    outputDir: String,
    fileHeader: String? = null,
    sortingRule: Comparator<BodyMemberDeclaration>? = null,
    init: JavaModuleInfoScope.() -> Unit
) : JavaModuleInfoScope {
    val cu = ModularCompilationUnit(name, sortingRule = sortingRule)

    return JavaModuleInfoScope(cu, sortingRule)
        .also(init)
        .also { Template(JAVA_ADAPTER, outputDir, "module-info.java", fileHeader, { beginModularCompilationUnit(cu) }).reg() }
}

@KratonDSL
class JavaModuleInfoScope internal constructor(
    override val compilationUnit: ModularCompilationUnit,
    private val sortingRule: Comparator<BodyMemberDeclaration>?,
    bodyMembers: MutableList<BodyMemberDeclaration> = compilationUnit.bodyMembers
) : JavaCompilationUnitScope<JavaModuleInfoScope>(compilationUnit, bodyMembers) {

    /**
     * TODO doc
     *
     * @since 1.0.0
     */
    override val documentation: JavaDocumentationScope
        get() = JavaDocumentationScope(compilationUnit.documentation)

    override fun setModifiers(vararg mods: JavaModifier) {
        mods.forEach {
            when (it) {
                is JavaLanguageModifier -> compilationUnit.modifiers.add(it.mod)
                is Annotate -> compilationUnit.annotations.add(Annotation(it.type, it.params))
                else -> throw IllegalArgumentException("Modifier $it may not be applied to module-info file")
            }
        }
    }

    fun requires(pack: String): JavaRequiresScope = JavaRequiresScope(ModuleRequiresDeclaration(pack).also { bodyMembers.add(it) })
    fun exports(pack: String): JavaExportsScope = JavaExportsScope(ModuleExportsDeclaration(pack).also { bodyMembers.add(it) })
    fun opens(pack: String): JavaOpensScope = JavaOpensScope(ModuleOpensDeclaration(pack).also { bodyMembers.add(it) })

    fun uses(service: IJvmType): JavaUsesScope = JavaUsesScope(ModuleUsesDeclaration(service).also { bodyMembers.add(it) })
        .apply { this@JavaModuleInfoScope.import(service) }

    fun provides(service: IJvmType, vararg impls: IJvmType): JavaProvidesScope {
        if (impls.isEmpty()) throw IllegalArgumentException()

        val decl = ModuleProvidesDeclaration(service, impls.toList())
        bodyMembers.add(decl)
        import(service)
        impls.forEach { import(it) }

        return JavaProvidesScope(decl)
    }

    infix fun JavaExportsScope.to(modules: Array<String>): JavaExportsScope = apply { decl.toModules.addAll(modules) }
    infix fun JavaOpensScope.to(modules: Array<String>): JavaOpensScope = apply { decl.toModules.addAll(modules) }

    /**
     * Creates, registers and returns a new [JavaModuleInfoScope] object.
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
     * @return the group's scope representation
     *
     * @since 1.0.0
     */
    override fun group(sortingRule: Comparator<BodyMemberDeclaration>?, init: JavaModuleInfoScope.() -> Unit): JavaModuleInfoScope =
        this.sortingRule?.let {
            GroupDeclaration(sortingRule).let {
                bodyMembers.add(it)

                JavaModuleInfoScope(compilationUnit, sortingRule, it.bodyMembers).also(init)
            }
        } ?: apply(init)

}

@KratonDSL class JavaRequiresScope internal constructor(internal val decl: ModuleRequiresDeclaration)
@KratonDSL class JavaExportsScope internal constructor(internal val decl: ModuleExportsDeclaration)
@KratonDSL class JavaOpensScope internal constructor(internal val decl: ModuleOpensDeclaration)
@KratonDSL class JavaUsesScope internal constructor(internal val decl: ModuleUsesDeclaration)
@KratonDSL class JavaProvidesScope internal constructor(internal val decl: ModuleProvidesDeclaration)