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

import com.github.themrmilchmann.kraton.lang.java.ast.Annotation
import com.github.themrmilchmann.kraton.lang.java.ast.CompilationUnit
import com.github.themrmilchmann.kraton.lang.jvm.*
import java.util.*

internal fun Collection<IJvmType>.joinAsString(compilationUnit: CompilationUnit?, delimiter: String = ", ") = StringJoiner(delimiter).run {
    this@joinAsString.forEach { add(it.asString(compilationUnit)) }
    toString()
}

@JvmName("joinAnnotationsAsString")
internal fun List<Annotation>.joinAsString(compilationUnit: CompilationUnit?, delimiter: String = " ") = StringJoiner(delimiter).run {
    this@joinAsString.forEach { add("@${it.type.asString(compilationUnit)}${it.params?.let { "($it)"} ?: ""}") }
    toString()
}

internal fun IJvmType.asString(from: CompilationUnit?): String = when {
    this is JvmArrayType        -> stringValueOfJvmArrayType(from)
    this is JvmGenericType      -> stringValueOfJvmGenericType(from)
    this is JvmPrimitiveType    -> stringValueOfPrimitiveType()
    this is JvmPrimitiveBoxType -> stringValueOfPrimitiveBoxType()
    this is AbstractJvmType     -> stringValueOfAbstractJvmType(from)
    else                        -> stringValueOfIJvmType(from)
}

private fun JvmArrayType.stringValueOfJvmArrayType(from: CompilationUnit?) =
    type.asString(from) + StringBuilder().run {
        for (i in 0 until dimensions) append("[]")
        toString()
    }

private fun JvmGenericType.stringValueOfJvmGenericType(from: CompilationUnit?) =
    className + StringBuilder().run {
        if (bounds.isNotEmpty()) {
            append(" ${if (upperBounds) "extends" else "super"} ")
            append(StringJoiner(" & ").run {
                for (bound in bounds) append(bound.asString(from))
                toString()
            })
        }

        toString()
    }

private fun JvmPrimitiveType.stringValueOfPrimitiveType() = when (this) {
    boolean -> "boolean"
    byte    -> "byte"
    char    -> "char"
    double  -> "double"
    float   -> "float"
    int     -> "int"
    long    -> "long"
    short   -> "short"
    else    -> throw UnsupportedOperationException()
}

private fun JvmPrimitiveBoxType.stringValueOfPrimitiveBoxType() = when (this) {
    boolean.box -> "Boolean"
    byte.box    -> "Byte"
    char.box    -> "Character"
    double.box  -> "Double"
    float.box   -> "Float"
    int.box     -> "Integer"
    long.box    -> "Long"
    short.box   -> "Short"
    else        -> throw UnsupportedOperationException()
}

private fun AbstractJvmType.stringValueOfAbstractJvmType(from: CompilationUnit?): String {
    val typeParameters = this.typeParameters
    val superString = stringValueOfIJvmType(from)

    return if (typeParameters == null || typeParameters.isEmpty())
        superString
    else
        StringBuilder().run {
            append(superString)
            append("<")
            append(StringJoiner(", ").run {
                typeParameters.forEach { add(it.asString(from)) }
                toString()
            })
            append(">")
            toString()
        }
}

private fun IJvmType.stringValueOfIJvmType(from: CompilationUnit?) =
    if (from != null && from.isResolved(this)) {
        memberName
    } else {
        packageName?.plus(".$memberName") ?: memberName
    }
