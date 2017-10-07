/*
 * Copyright (c) 2017 Leon Linhart,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.themrmilchmann.kraton.lang.java

import com.github.themrmilchmann.kraton.lang.jvm.*
import java.util.*

fun IJvmType.asString(from: JavaTopLevelType<*, *>?): String = when {
    this is JvmArrayType        -> stringValueOfJvmArrayType(from)
    this is JvmGenericType      -> stringValueOfJvmGenericType(from)
    this is JvmPrimitiveType    -> stringValueOfPrimitiveType()
    this is JvmPrimitiveBoxType -> stringValueOfPrimitiveBoxType()
    this is AbstractJvmType     -> stringValueOfAbstractJvmType(from)
    else                        -> stringValueOfIJvmType(from)
}

private fun JvmArrayType.stringValueOfJvmArrayType(from: JavaTopLevelType<*, *>?) =
    type.asString(from) + StringBuilder().run {
        for (i in 0 until dimensions) append("[]")
        toString()
    }

private fun JvmGenericType.stringValueOfJvmGenericType(from: JavaTopLevelType<*, *>?) =
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
    char.box    -> "Char"
    double.box  -> "Double"
    float.box   -> "Float"
    int.box     -> "Integer"
    long.box    -> "Long"
    short.box   -> "Short"
    else        -> throw UnsupportedOperationException()
}

private fun AbstractJvmType.stringValueOfAbstractJvmType(from: JavaTopLevelType<*, *>?): String {
    val typeParameters = this.typeParameters
    val superString = stringValueOfIJvmType(from)

    return if (typeParameters == null || typeParameters.isEmpty())
        superString
    else
        StringBuilder().run {
            append(superString)
            append("<")
            append(StringJoiner(",").run {
                typeParameters.forEach { add(it.asString(from)) }
                toString()
            })
            append(">")
            toString()
        }
}

private fun IJvmType.stringValueOfIJvmType(from: JavaTopLevelType<*, *>?) =
    if (from != null && from.isResolved(this)) {
        memberName
    } else {
        packageName?.plus(".$memberName") ?: memberName
    }

/**
 * Casts a value to another type (excluding unnecessary casts).
 *
 * @param from  the initial type
 * @param to    the desired type
 * @param value the value to be cast
 *
 * @return the casted value
 *
 * @since 1.0.0
 */
fun cast(from: AbstractJvmType, to: AbstractJvmType, value: String): String {
    if (from is JvmPrimitiveType && to is JvmPrimitiveType) {
        when (to) {
            byte  -> when (from) {
                short, int, char, long -> return "($to) $value"
            }
            short -> when (from) {
                int, char, long -> return "($to) $value"
            }
            int   -> when (from) {
                long, float, double -> return "($to) $value"
            }
            char  -> when (from) {
                byte, short, int, long, float, double -> return "($to) $value"
            }
            long  -> when (from) {
                float, double -> return "($to) $value"
            }
            float -> when (from) {
                double -> return "($to) $value"
            }
        }

        return value
    }

    return "($to) $value"
}

/**
 * Converts a value to another type.
 *
 * @param from  the initial type
 * @param to    the desired type
 * @param value the value to be converted
 *
 * @return the converted value
 *
 * @since 1.0.0
 */
fun convert(from: AbstractJvmType, to: AbstractJvmType, value: String): String {
    if (from is JvmPrimitiveType && to is JvmPrimitiveType) {
        when (from) {
            boolean -> when (to) {
                byte, short, int, float, double, long -> return cast(int, to, "($value ? 1 : ${to.nullValue})")
                char                                  -> return "$value ? '\\u0001' : ${to.nullValue}"
            }
            float   -> when (to) {
                byte, short -> return convert(int, to, "Float.floatToRawIntBits($value)")
                int, long   -> return "Float.floatToRawIntBits($value)"
            }
            double  -> when (to) {
                byte, short, int -> return convert(long, to, "Double.doubleToRawLongBits($value)")
                long             -> return "Double.doubleToRawLongBits($value)"
            }
        }

        when (to) {
            boolean -> return "$value != ${from.nullValue}"
            float   -> when (from) {
                byte, short, int, char -> return "Float.intBitsToFloat($value)"
            }
            double  -> when (from) {
                byte, short, int, char, long -> return "Double.longBitsToDouble($value)"
            }
        }
    }

    return cast(from, to, value)
}