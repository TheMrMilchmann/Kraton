/*
 * Copyright (c) 2017 Leon Linhart,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
package com.github.themrmilchmann.kraton.lang.jvm

import kotlin.reflect.*

/**
 * Creates and returns a new [IJvmType] referring to the receiver type.
 *
 * <b>This should be used with care. Types that are visible for the templates
 * may not be available for the generated output and vice-versa.</b>
 *
 * @receiver the type to create a reference to
 *
 * @since 1.0.0
 */
val Class<*>.asType: IJvmType get() = this.asType()

/**
 * Creates and returns a new [IJvmType] referring to the receiver type.
 *
 * <b>This should be used with care. Types that are visible for the templates
 * may not be available for the generated output and vice-versa.</b>
 *
 * @receiver the type to create a reference to
 *
 * @param typeParameters the type-parameters for the type
 *
 * @return the newly created [IJvmType]
 *
 * @since 1.0.0
 */
fun Class<*>.asType(vararg typeParameters: IJvmType): IJvmType =
    if (this.isMemberClass)
        enclosingClass.asType.member(simpleName, *typeParameters)
    else
        JvmTypeReference(simpleName, `package`.name, *typeParameters)

/**
 * Creates and returns a new [IJvmType] referring to the receiver type.
 *
 * <b>This should be used with care. Types that are visible for the templates
 * may not be available for the generated output and vice-versa.</b>
 *
 * @receiver the type to create a reference to
 *
 * @since 1.0.0
 */
val KClass<*>.asType: IJvmType get() = java.asType

/**
 * Creates and returns a new [IJvmType] referring to the receiver type.
 *
 * <b>This should be used with care. Types that are visible for the templates
 * may not be available for the generated output and vice-versa.</b>
 *
 * @receiver the type to create a reference to
 *
 * @param typeParameters the type-parameters for the type
 *
 * @return the newly created [IJvmType]
 *
 * @since 1.0.0
 */
fun KClass<*>.asType(vararg typeParameters: IJvmType): IJvmType =
    java.asType(*typeParameters)

fun IJvmType.member(className: String, vararg typeParameters: IJvmType) =
    object: JvmTypeReference(className, "", *typeParameters) {

        override val enclosingType get() = this@member
        override val packageName get() = enclosingType.packageName

    }

/**
 * TODO doc
 *
 * @since 1.0.0
 */
interface IJvmType {

    /**
     * TODO doc
     *
     * @since 1.0.0
     */
    val enclosingType: IJvmType? get() = null

    /**
     * TODO doc
     *
     * @since 1.0.0
     */
    val packageName: String? get() = enclosingType?.packageName

    /**
     * TODO doc
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

    /**
     * TODO doc
     *
     * @since 1.0.0
     */
    val isNullable: Boolean get() = false

}
/**
 * TODO doc
 *
 * @property className the name of the type
 *
 * @constructor Creates a new reference to a java type.
 *
 * @since 1.0.0
 */
abstract class AbstractJvmType internal constructor(
    override val className: String,
    val typeParameters: Array<out IJvmType>? = null
): IJvmType {

    override val packageName: String? = null

}

/**
 * TODO doc
 *
 * @property packageName the name of the package of the represented type
 *
 * @since 1.0.0
 */
open class JvmTypeReference(
    className: String,
    override val packageName: String?,
    vararg typeParameters: IJvmType,
    override val isNullable: Boolean = false
): AbstractJvmType(className, typeParameters)

/**
 * TODO doc
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
fun IJvmType.array(dim: Int = 1) = JvmArrayType(this, dim)

/**
 * TODO doc
 *
 * @property type       the type of the array
 * @property dimensions the dimensions of the array (defaults to one)
 *
 * @constructor Creates a new array representing type with the given type and
 *              dimensions.
 *
 * @since 1.0.0
 */
class JvmArrayType(
    val type: IJvmType,
    val dimensions: Int
): IJvmType by type

/**
 * TODO doc
 *
 * @since 1.0.0
 */
class JvmGenericType(
    name: String,
    vararg val bounds: IJvmType,
    val upperBounds: Boolean = true
): AbstractJvmType(name)

/**
 * A JvmPrimitive type represents a primitive type.
 **
 * @property nullValue  the `null`-value of this primitive type
 * @property size       the size (in bytes) of this type
 * @property abbrevName a commonly used abbreviation of this type's name
 *
 * @param name the boxed type of this primitive type
 *
 * @since 1.0.0
 */
class JvmPrimitiveType internal constructor(
    name: String,
    val nullValue: String,
    val size: Int,
    val abbrevName: String = name
): AbstractJvmType(name) {

    val box = JvmPrimitiveBoxType(name)

    override val packageName = "java.lang"

}

class JvmPrimitiveBoxType internal constructor(
    name: String
): AbstractJvmType(name)

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
fun smaller(alpha: JvmPrimitiveType, beta: JvmPrimitiveType) = if (alpha.size > beta.size) beta else alpha

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
fun larger(alpha: JvmPrimitiveType, beta: JvmPrimitiveType) = if (alpha.size < beta.size) beta else alpha