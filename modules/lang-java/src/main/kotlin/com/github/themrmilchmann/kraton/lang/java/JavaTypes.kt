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

/**
 * The `void` type.
 *
 * @since 1.0.0
 */
object void: JavaReferableType("void")

/** The `boolean` type.
 *
 * @since 1.0.0
 */
val boolean = JavaPrimitiveType("Boolean", "boolean", "false", -1)

/**
 * The `byte` type.
 *
 * @since 1.0.0
 */
val byte = JavaPrimitiveType("Byte", "byte", "0", 8)

/**
 * The `short` type.
 *
 * @since 1.0.0
 */
val short = JavaPrimitiveType("Short", "short", "0", 16)

/**
 * The `char` type.
 *
 * @since 1.0.0
 */
val char = JavaPrimitiveType("Character", "char", "'\\u0000'", 32, "Char")

/**
 * The `int` type.
 *
 * @since 1.0.0
 */
val int = JavaPrimitiveType("Integer", "int", "0", 32, "Int")

/**
 * The `float` type.
 *
 * @since 1.0.0
 */
val float = JavaPrimitiveType("Float", "float", "0F", 32)

/**
 * The `long` type.
 *
 * @since 1.0.0
 */
val long = JavaPrimitiveType("Long", "long", "0L", 64)

/**
 * The `double` type.
 *
 * @since 1.0.0
 */
val double = JavaPrimitiveType("Double", "double", "0D", 64)

/**
 * The `String` type.
 *
 * @since 1.0.0
 */
val string = JavaTypeReference("String", null)

/**
 * A reference to a java type.
 *
 * @since 1.0.0
 */
interface IJavaType {

    /**
     * Returns the name of the package containing this type (or `null` for "java.lang" and primitives types).
     *
     * @return the name of the package containing this type (or `null` for "java.lang" and primitives types)
     *
     * @since 1.0.0
     */
	fun toPackageString(): String?

    /**
     * Returns the qualified name of this type. That is, the name of this type dot prefixed by enclosing types.
     *
     * @return the qualified name of this type
     *
     * @since 1.0.0
     */
	fun toQualifiedString(): String

    /**
     * Returns the name of this type.
     *
     * @return the name of this type
     *
     * @since 1.0.0
     */
	override fun toString(): String

}

/**
 * A reference to a java type.
 *
 * @property className the name of the type
 * @constructor Creates a new reference to a java type.
 *
 * @since 1.0.0
 */
abstract class JavaReferableType(
	val className: String
): IJavaType {

	override fun toPackageString(): String? = null

	override fun toQualifiedString() = toString()

	override fun toString() = className

}

/**
 * A reference to a java type.
 *
 * @property packageName the name of the package of the represented type
 *
 * @since 1.0.0
 */
class JavaTypeReference(
	className: String,
	private val packageName: String?
): JavaReferableType(className) {

	override fun toPackageString() = packageName

}

/**
 * Shortcut to create a new JavaArrayType.
 *
 * Creates a new array representing type with the given type and dimensions.
 *
 * @receiver the type of the array
 * @param dim the dimensions of the array (defaults to one)
 * @return the representing type
 *
 * @since 1.0.0
 */
fun JavaReferableType.array(dim: Int = 1) = JavaArrayType(this, dim)

/**
 * A type representing a java array.
 *
 * @property type the type of the array
 * @property dim the dimensions of the array (defaults to one)
 * @constructor Creates a new array representing type with the given type and dimensions.
 *
 * @since 1.0.0
 */
class JavaArrayType(
	val type: IJavaType,
	val dim: Int = 1
): JavaReferableType(type.toString()) {

	override fun toPackageString() = type.toPackageString()

	override fun toQualifiedString() = type.toQualifiedString()

	override fun toString() = super.toString().plus(StringBuilder().run {
		for (i in 0 until dim) append("[]")
		toString()
	})

}

/**
 * A java generic type.
 *
 * @param name the name of the generic type
 * @property bounds the bounds of this type
 *
 * @since 1.0.0
 */
class JavaGenericType(
	name: String,
	private vararg val bounds: IJavaType
): JavaReferableType(name) {

	override fun toString() = className.plus(StringBuilder().run {
		if (bounds.isNotEmpty()) {
			append(" extends ")
			append(StringJoiner(",").run {
				for (bound in bounds) append(bound)
				toString()
			})
		}

		toString()
	})

}

/**
 * A JavaPrimitiveType represents a primitive type.
 *
 * @property boxedType the boxed type of this primitive type
 * @property className the name of this type
 * @property nullValue the `null`-value of this primitive type
 * @property size the size (in bytes) of this type
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
 * @param from the initial type
 * @param to the desired type
 * @param value the value to be cast
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
 * @param from the initial type
 * @param to the desired type
 * @param value the value to be converted
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
 * @param beta type to be compared
 * @return the type which size in bytes is smaller (`alpha` is also returned if there is no difference)
 *
 * @since 1.0.0
 */
fun smaller(alpha: JavaPrimitiveType, beta: JavaPrimitiveType) = if (alpha.size > beta.size) beta else alpha

/**
 * Compares two types by size in bytes and returns the larger one.
 *
 * @param alpha type to be compared
 * @param beta type to be compared
 * @return the type which size in bytes is larger (`alpha` is also returned if there is no difference)
 *
 * @since 1.0.0
 */
fun larger(alpha: JavaPrimitiveType, beta: JavaPrimitiveType) = if (alpha.size < beta.size) beta else alpha