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

import com.github.themrmilchmann.kraton.io.*
import com.github.themrmilchmann.kraton.lang.java.impl.model.toModifierString
import java.io.*

internal class JavaPrinter(writer: BufferedWriter) : KPrinter(writer) {

    fun beginOrdinaryCompilationUnit(decl: OrdinaryCompilationUnit) {
        // TODO setup printer
        decl.print()
    }

    fun List<TypeParameter>.print(scope: OrdinaryCompilationUnit?) {
        if (isNotEmpty()) {
            print("<")
            print(joinToString(", ") {
                it.identifier
                    .run { if (it.bounds.isNotEmpty()) "$this ${if (it.upperBounds) "extends" else "super"} ${it.bounds.joinAsString(scope, " & ")}" else this }
                    .run { if (it.annotations.isNotEmpty()) "${it.annotations.joinAsString(scope)} $this" else this }
            })
            print(">")
        }
    }

    // Printer DSL for the AST

    private fun BodyMemberDeclaration.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        when (this) {
            is TypeDeclaration -> print(scope, prev, next)
            is FieldDeclaration -> print(scope, prev, next)
            is MethodDeclaration -> print(scope, prev, next)
            is ConstructorDeclaration -> print(scope, prev, next)
        }
    }

    private fun List<BodyMemberDeclaration>.print(scope: OrdinaryCompilationUnit, sortingRule: Comparator<BodyMemberDeclaration>?) {
        (sortingRule?.let { sortedWith(sortingRule) } ?: this).mapIndexed { i, it -> (if (i == 0) null else this[i - 1]) to it }
            .mapIndexed { i, it -> it to (if (i == size - 1) null else this[i + 1]) }
            .forEach { it.first.second.print(scope, it.first.first, it.second) }
    }

    private fun TypeDeclaration.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        when (this) {
            is NormalClassDeclaration -> print(scope, prev, next)
            is NormalInterfaceDeclaration -> print(scope, prev, next)
        }
    }

    private fun GroupDeclaration.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        if (bodyMembers.isNotEmpty()) bodyMembers.print(scope, sortingRule)
    }

    private fun OrdinaryCompilationUnit.print() {
        packageDeclaration?.apply {
            print()
            println()
        }
        importDeclarations
            .values
            .flatMap { it.values }
            .sortedWith(Comparator { a, b -> "${a.container}.${a.member}".compareTo("${b.container}.${b.member}") })
            .forEach { if (!it.isImplicit) it.print() }
        if (importDeclarations.any { it.value.any { !it.value.isImplicit } }) println()
        typeDeclaration.print(this@print, null, null)
    }

    private fun PackageDeclaration.print() {
        printI("package ")
        print(identifiers.toQualifiedString())
        println(";")
    }

    fun ImportDeclaration.print() {
        printI("import ")
        if (isStatic) print("static ")
        println("$container.$member;")
    }


    private fun NormalClassDeclaration.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        documentation.print(scope)
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
            bodyMembers.print(scope, sortingRule)
            decIndent()
            print(indent)
        }
        print("}")
        if (scope.typeDeclaration !== this) println("$ln")
    }

    private fun NormalInterfaceDeclaration.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        documentation.print(scope)
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
            bodyMembers.print(scope, sortingRule)
            decIndent()
            print(indent)
        }
        print("}")
        if (scope.typeDeclaration !== this) println("$ln")
    }

    private fun FieldDeclaration.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        documentation.print(scope)
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

    private fun MethodDeclaration.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        if (prev is FieldDeclaration) println()

        documentation.print(scope)
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

    private fun ConstructorDeclaration.print(scope: OrdinaryCompilationUnit, prev: BodyMemberDeclaration?, next: BodyMemberDeclaration?) {
        if (prev is FieldDeclaration) println()

        documentation.print(scope)
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

    fun Documentation.print(scope: OrdinaryCompilationUnit?) {
        if (authors.isNotEmpty()
            || params.any { it.value.isNotEmpty() }
            || typeParams.any { it.value.isNotEmpty() }
            || content !== null
            || returnDoc !== null
            || exceptions.any { it.value.isNotEmpty() }
            || see.isNotEmpty()
            || since !== null) {
            print(StringBuilder().apply {
                content?.let { append(it.cleanup("$indent * ")) }

                if (typeParams.any { it.value.isNotEmpty() }) {
                    if (isNotEmpty()) append("$ln$indent *")
                    typeParams.filter { it.value.isNotEmpty() }.forEach { key, value ->
                        if (isNotEmpty()) append("$ln$indent * ")
                        append("@param <${key.identifier}> $value")
                    }
                }

                if (params.any { it.value.isNotEmpty() }) {
                    if (isNotEmpty()) append("$ln$indent *")
                    params.filter { it.value.isNotEmpty() }.forEach { key, value ->
                        if (isNotEmpty()) append("$ln$indent * ")
                        append("@param <${key.name}> $value")
                    }
                }

                returnDoc?.let {
                    if (isNotEmpty()) append("$ln$indent *$ln$indent *")
                    append("@return $it")
                }

                if (exceptions.any { it.value.isNotEmpty() }) {
                    if (isNotEmpty()) append("$ln$indent *")
                    typeParams.filter { it.value.isNotEmpty() }.forEach { key, value ->
                        if (isNotEmpty()) append("$ln$indent * ")
                        append("@throws ${key.identifier} $value")
                    }
                }

                if (see.isNotEmpty()) {
                    if (isNotEmpty()) append("$ln$indent *")
                    see.forEach {
                        if (isNotEmpty()) append("$ln$indent * ")
                        append("@see $it$")
                    }
                }

                since?.let {
                    if (isNotEmpty()) append("$ln$indent *$ln$indent * ")
                    append("@since $it")
                }

                if (authors.isNotEmpty()) {
                    if (isNotEmpty()) append("$ln$indent *")
                    authors.forEach {
                        if (isNotEmpty()) append("$ln$indent * ")
                        append("@author $it")
                    }
                }
            }.toString().layoutJavadoc(indent) + ln)
        }
    }

}