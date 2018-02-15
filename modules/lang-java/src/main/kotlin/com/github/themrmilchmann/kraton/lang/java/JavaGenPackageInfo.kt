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
import com.github.themrmilchmann.kraton.lang.java.ast.BodyMemberDeclaration
import com.github.themrmilchmann.kraton.lang.java.ast.PackageInfo
import com.github.themrmilchmann.kraton.lang.java.impl.*

fun TemplateFile.javaPackageInfo(
    name: String,
    outputDir: String,
    fileHeader: String? = null,
    init: JavaPackageInfoScope.() -> Unit
) : JavaPackageInfoScope {
    val packageInfo = PackageInfo(name)

    return JavaPackageInfoScope(packageInfo)
        .also(init)
        .also { Template(JAVA_ADAPTER, outputDir, "$name/package-info.java", fileHeader, { beginPackageInfo(packageInfo) }).reg() }
}

@KratonDSL
class JavaPackageInfoScope internal constructor(
    override val compilationUnit: PackageInfo
) : JavaCompilationUnitScope<JavaPackageInfoScope>(compilationUnit, mutableListOf()) {

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
                is Annotate -> compilationUnit.annotations.add(Annotation(it.type, it.params))
                else -> throw IllegalArgumentException("Modifier $it may not be applied to module-info file")
            }
        }
    }

    override fun group(sortingRule: Comparator<BodyMemberDeclaration>?, init: JavaPackageInfoScope.() -> Unit): JavaPackageInfoScope =
        throw UnsupportedOperationException()

}
