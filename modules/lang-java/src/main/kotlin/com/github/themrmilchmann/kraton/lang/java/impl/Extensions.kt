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

import com.github.themrmilchmann.kraton.lang.java.ast.*
import com.github.themrmilchmann.kraton.lang.java.impl.model.*
import com.github.themrmilchmann.kraton.lang.jvm.*

internal fun CompilationUnit.isImported(type: IJvmType) =
    importDeclarations[type.packageName]?.any { it.value.member === IMPORT_WILDCARD || it.value.member == type.className } ?: false

internal fun CompilationUnit.isResolved(type: IJvmType): Boolean {
    return when (this) {
        is OrdinaryCompilationUnit -> isResolved(type)
        else -> isImported(type)
    }
}

internal fun OrdinaryCompilationUnit.isResolved(type: IJvmType): Boolean {
    return isImported(type) || (packageDeclaration!!.equalsPackage(type))
}

internal fun CompilationUnit.import(container: String, member: String, mode: ImportType?, isStatic: Boolean, isImplicit: Boolean) {
    when (this) {
        is OrdinaryCompilationUnit -> import(container, member, mode, isStatic, isImplicit)
        else -> {
            if (mode == null) {
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
    }
}

internal fun OrdinaryCompilationUnit.import(container: String, member: String, mode: ImportType?, isStatic: Boolean, isImplicit: Boolean) {
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