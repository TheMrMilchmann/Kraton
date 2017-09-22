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

import java.util.*
import kotlin.reflect.*

/**
 * Creates and returns a new [IJavaType] referring to the receiver type.
 *
 * <b>This should be used with care. Types that are visible for the templates
 * may not be available for the generated output and vice-versa.</b>
 *
 * @receiver the type to create a reference to
 *
 * @since 1.0.0
 */
val Class<*>.asType: IJavaType get() = this.asType()

/**
 * Creates and returns a new [IJavaType] referring to the receiver type.
 *
 * <b>This should be used with care. Types that are visible for the templates
 * may not be available for the generated output and vice-versa.</b>
 *
 * @receiver the type to create a reference to
 *
 * @param typeParameters the type-parameters for the type
 *
 * @return the newly created [IJavaType]
 *
 * @since 1.0.0
 */
fun Class<*>.asType(vararg typeParameters: IJavaType): IJavaType =
    if (this.isMemberClass)
        enclosingClass.asType.member(simpleName, *typeParameters)
    else
        JavaTypeReference(simpleName, `package`.name, *typeParameters)

/**
 * Creates and returns a new [IJavaType] referring to the receiver type.
 *
 * <b>This should be used with care. Types that are visible for the templates
 * may not be available for the generated output and vice-versa.</b>
 *
 * @receiver the type to create a reference to
 *
 * @since 1.0.0
 */
val KClass<*>.asType: IJavaType get() = java.asType

/**
 * Creates and returns a new [IJavaType] referring to the receiver type.
 *
 * <b>This should be used with care. Types that are visible for the templates
 * may not be available for the generated output and vice-versa.</b>
 *
 * @receiver the type to create a reference to
 *
 * @param typeParameters the type-parameters for the type
 *
 * @return the newly created [IJavaType]
 *
 * @since 1.0.0
 */
fun KClass<*>.asType(vararg typeParameters: IJavaType): IJavaType =
    java.asType(*typeParameters)

fun IJavaType.member(className: String, vararg typeParameters: IJavaType) =
    object: JavaTypeReference(className, "", *typeParameters) {

        override val enclosingType get() = this@member
        override val packageName get() = enclosingType.packageName

    }

/**
 * A reference to a java type.
 *
 * @since 1.0.0
 */
interface IJavaType {

    val enclosingType: IJavaType? get() = null

    /**
     * The name of the package containing this type. (May be `null`.)
     *
     * @since 1.0.0
     */
    val packageName: String? get() = enclosingType?.packageName
    /**
     * The simple name of the class represented by this type.
     *
     * @since 1.0.0
     */
    val className: String

    /**
     * The class name of the outermost enclosing type.
     *
     * @since 1.0.0
     */
    val containerName: String get() = enclosingType?.className ?: className
    /**
     * The name of this class dot prefixed with the name of it's enclosing type.
     *
     * @since 1.0.0
     */
    val memberName: String get() = enclosingType?.memberName?.plus(".$className") ?: className

    fun asString(from: JavaTopLevelType?) =
        if (from != null && from.isResolved(this)) {
            memberName
        } else {
            packageName?.plus(".$memberName") ?: memberName
        }

}

/**
 * A reference to a java type.
 *
 * @property className the name of the type
 *
 * @constructor Creates a new reference to a java type.
 *
 * @since 1.0.0
 */
abstract class JavaReferableType internal constructor(
    override val className: String,
    private val typeParameters: Array<out IJavaType>? = null
): IJavaType {

    override val packageName: String? = null

    override fun asString(from: JavaTopLevelType?) =
        if (typeParameters == null || typeParameters.isEmpty())
            super.asString(from)
        else
            StringBuilder().run {
                append(super.asString(from))
                append("<")
                append(StringJoiner(",").run {
                    typeParameters.forEach { add(it.asString(from)) }
                    toString()
                })
                append(">")
                toString()
            }

    override fun toString() = memberName

}

/**
 * A reference to a java type.
 *
 * @property packageName the name of the package of the represented type
 *
 * @since 1.0.0
 */
open class JavaTypeReference(
    className: String,
    override val packageName: String?,
    vararg typeParameters: IJavaType
): JavaReferableType(className, typeParameters)

/**
 * Shortcut to create a new JavaArrayType.
 *
 * Creates a new array representing type with the given type and dimensions.
 *
 * @receiver the type of the array
 *
 * @param dim the dimensions of the array (defaults to one)
 *
 * @return the representing type
 *
 * @since 1.0.0
 */
fun IJavaType.array(dim: Int = 1) = JavaArrayType(this, dim)

/**
 * A type representing a java array.
 *
 * @property type   the type of the array
 * @property dim    the dimensions of the array (defaults to one)
 *
 * @constructor Creates a new array representing type with the given type and dimensions.
 *
 * @since 1.0.0
 */
class JavaArrayType(
    val type: IJavaType,
    val dim: Int = 1
): IJavaType by type {

    override fun asString(from: JavaTopLevelType?) =
        super.asString(from) + StringBuilder().run {
            for (i in 0 until dim) append("[]")
            toString()
        }

}

/**
 * A java generic type.
 *
 * @param name      the name of the generic type
 * @property bounds the bounds of this type
 *
 * @since 1.0.0
 */
class JavaGenericType(
    name: String,
    private vararg val bounds: IJavaType,
    private val upperBounds: Boolean = true
): JavaReferableType(name) {

    override fun asString(from: JavaTopLevelType?) =
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

}

/**
 * A JavaPrimitiveType represents a primitive type.
 *
 * @property boxedType  the boxed type of this primitive type
 * @property className  the name of this type
 * @property nullValue  the `null`-value of this primitive type
 * @property size       the size (in bytes) of this type
 * @property abbrevName a commonly used abbreviation of this type's name
 *
 * @since 1.0.0
 */
class JavaPrimitiveType private constructor(
    val boxedType: JavaReferableType,
    className: String,
    val nullValue: String,
    val size: Int,
    val abbrevName: String
): JavaReferableType(className) {

    internal constructor(boxedType: String, className: String, nullValue: String, size: Int = -1, abbrevName: String = boxedType):
        this(JavaTypeReference(boxedType, null), className, nullValue, size, abbrevName)

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
fun cast(from: JavaReferableType, to: JavaReferableType, value: String): String {
    if (from is JavaPrimitiveType && to is JavaPrimitiveType) {
        when (to) {
            byte -> when (from) {
                short, int, char, long -> return "($to) $value"
            }
            short -> when (from) {
                int, char, long -> return "($to) $value"
            }
            int -> when (from) {
                long, float, double -> return "($to) $value"
            }
            char -> when(from) {
                byte, short, int, long, float, double -> return "($to) $value"
            }
            long -> when (from) {
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
fun convert(from: JavaReferableType, to: JavaReferableType, value: String): String {
    if (from is JavaPrimitiveType && to is JavaPrimitiveType) {
        when (from) {
            boolean -> when (to) {
                byte, short, int, float, double, long -> return cast(int, to, "($value ? 1 : ${to.nullValue})")
                char -> return "$value ? '\\u0001' : ${to.nullValue}"
            }
            float -> when (to) {
                byte, short -> return convert(int, to, "Float.floatToRawIntBits($value)")
                int, long -> return "Float.floatToRawIntBits($value)"
            }
            double -> when (to) {
                byte, short, int -> return convert(long, to, "Double.doubleToRawLongBits($value)")
                long -> return "Double.doubleToRawLongBits($value)"
            }
        }

        when (to) {
            boolean -> return "$value != ${from.nullValue}"
            float -> when (from) {
                byte, short, int, char -> return "Float.intBitsToFloat($value)"
            }
            double -> when (from) {
                byte, short, int, char, long -> return "Double.longBitsToDouble($value)"
            }
        }
    }

    return cast(from, to, value)
}

/**
 * Compares two types by size in bytes and returns the smaller one.
 *
 * @param alpha type to be compared
 * @param beta  type to be compared
 *
 * @return the type which size in bytes is smaller (`alpha` is also returned if there is no difference)
 *
 * @since 1.0.0
 */
fun smaller(alpha: JavaPrimitiveType, beta: JavaPrimitiveType) = if (alpha.size > beta.size) beta else alpha

/**
 * Compares two types by size in bytes and returns the larger one.
 *
 * @param alpha type to be compared
 * @param beta  type to be compared
 *
 * @return the type which size in bytes is larger (`alpha` is also returned if there is no difference)
 *
 * @since 1.0.0
 */
fun larger(alpha: JavaPrimitiveType, beta: JavaPrimitiveType) = if (alpha.size < beta.size) beta else alpha