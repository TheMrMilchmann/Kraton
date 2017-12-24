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

/*
 * The classes in this file are used to represent types that may be used by any
 * JVM language module. Since a type's source representation may differ from
 * language to language the `lang-jvm-base` module does not provide any
 * capabilities that could be used to make types printable. Such capabilities
 * should be located in a `[Language]Types` module within the respective
 * module's primary package.
 * (See: `com.github.themrmilchmann.kraton.lang.java.JavaTypes.kt`)
 *
 * Global type definitions (type definitions for primitives and frequently used
 * types) may be found in `JvmGlobalTypes.kt`.
 *
 * Note that these types are not to be confused with types that are visible by
 * the JVM itself, but rather by any language built on top of the JVM.
 */

/**
 * Creates and returns a new `IJvmType` which holds a reference to the receiver
 * type of this property.
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
 * Creates and returns a new `IJvmType` which holds a reference to the receiver
 * type of this function.
 *
 * <b>This should be used with care. Types that are visible for the templates
 * may not be available for the generated output and vice-versa.</b>
 *
 * @receiver the type to create a reference to
 *
 * @param typeParameters    the type-parameters for the type
 * @param nullable          whether or not the referenced type is nullable
 *
 * @return the newly created `IJvmType`
 *
 * @since 1.0.0
 */
@JvmOverloads
fun Class<*>.asType(vararg typeParameters: IJvmType, nullable: Boolean = false): IJvmType =
    if (this.isMemberClass)
        enclosingClass.asType.member(simpleName, *typeParameters, nullable = nullable)
    else
        JvmTypeReference(simpleName, `package`.name, *typeParameters, nullable = nullable)

/**
 * Creates and returns a new `IJvmType` which holds a reference to the receiver
 * type of this property.
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
 * Creates and returns a new `IJvmType` which holds a reference to the receiver
 * type.
 *
 * <b>This should be used with care. Types that are visible for the templates
 * may not be available for the generated output and vice-versa.</b>
 *
 * @receiver the type to create a reference to
 *
 * @param typeParameters    the type-parameters for the type
 * @param nullable          whether or not the referenced type is nullable
 *
 * @return the newly created `IJvmType`
 *
 * @since 1.0.0
 */
@JvmOverloads
fun KClass<*>.asType(vararg typeParameters: IJvmType, nullable: Boolean = false): IJvmType =
    java.asType(*typeParameters, nullable = nullable)

/**
 * Creates and returns a new `IJvmType` which holds a reference to a member of
 * the receiver type.
 *
 * @receiver the type that contains the member to create a reference to
 *
 * @param className         the name of the member to be referenced
 * @param typeParameters    the type-parameters for the type
 * @param nullable          whether or not the referenced type is nullable
 *
 * @return the newly created `IJvmType`
 *
 * @since 1.0.0
 */
fun IJvmType.member(className: String, vararg typeParameters: IJvmType, nullable: Boolean = false) =
    JvmMemberType(this, className, typeParameters, nullable)

// Cannot inline (potentially related to KT-10835)
class JvmMemberType internal constructor(
    container: IJvmType,
    className: String,
    typeParameters: Array<out IJvmType>,
    nullable: Boolean
) : JvmTypeReference(className, "", *typeParameters, nullable = nullable) {

    override val enclosingType = container
    override val packageName = enclosingType.packageName

}

/**
 * An `IJvmType` represents a type that may be used in a JVM language.
 *
 * @since 1.0.0
 */
interface IJvmType {

    /**
     * The enclosing type of this type, if any.
     *
     * @since 1.0.0
     */
    val enclosingType: IJvmType? get() = null

    /**
     * The name of the package in which this type is located.
     *
     * @since 1.0.0
     */
    val packageName: String? get() = enclosingType?.packageName

    /**
     * The immediate name of the class of the represented type.
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
     * Whether or not the referenced type is nullable.
     *
     * This may or may not impact the resulting output depending on language and
     * configuration.
     *
     * @since 1.0.0
     */
    val isNullable: Boolean get() = false

    /**
     * Returns a nullable version of this `IJVMType`.
     *
     * If the current type is already nullable, it should simply be returned.
     *
     * @return a nullable version of this type
     *
     * @since 1.0.0
     */
    val nullable: IJvmType

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
abstract class AbstractJvmType internal constructor(
    override val className: String,
    val typeParameters: Array<out IJvmType>? = null,
    override val isNullable: Boolean = false
) : IJvmType {

    override val packageName: String? = null

}

/**
 * A reference to a java type.
 *
 * @param nullable whether or not the referenced type is nullable
 *
 * @property packageName the name of the package of the represented type
 *
 * @since 1.0.0
 */
open class JvmTypeReference(
    className: String,
    override val packageName: String?,
    vararg typeParameters: IJvmType,
    nullable: Boolean = false
) : AbstractJvmType(className, typeParameters, nullable) {

    override val nullable =
        if (isNullable)
            this
        else
            JvmTypeReference(className, packageName, *typeParameters!!, nullable = true)

}

/**
 * Shortcut to create a new `JvmArrayType`.
 *
 * Creates a new array representing type with the given type and dimensions.
 *
 * @receiver the type of the array
 *
 * @param dim       the dimensions of the array (defaults to one)
 * @param nullable  whether or not the referenced type is nullable
 *
 * @return the representing type
 *
 * @since 1.0.0
 */
@JvmOverloads
fun IJvmType.array(dim: Int = 1, nullable: Boolean = isNullable) = JvmArrayType(this, dim, nullable)

/**
 * A type representing an array.
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
    val dimensions: Int,
    override val isNullable: Boolean = type.isNullable
) : IJvmType by type {

    override val nullable =
        if (isNullable)
            this
        else
            JvmArrayType(type, dimensions, true)

}

/**
 * A type representing generic type.
 *
 * @since 1.0.0
 */
class JvmGenericType(
    name: String,
    vararg val bounds: IJvmType,
    val upperBounds: Boolean = true,
    override val isNullable: Boolean = false
) : AbstractJvmType(name) {

    override val nullable =
        if (isNullable)
            this
        else
            JvmGenericType(className, *bounds, upperBounds = upperBounds, isNullable = true)

}

/**
 * A `JvmPrimitive` type represents a primitive type.
 **
 * @property nullValue  the `null`-value of this primitive type
 * @property size       the size (in bytes) of this type
 * @property abbrevName a commonly used abbreviation of this type's name
 *
 * @param name the boxed type of this primitive type
 *
 * @since 1.0.0
 */
class JvmPrimitiveType private constructor(
    name: String,
    val nullValue: String,
    val size: Int,
    val abbrevName: String,
    override val isNullable: Boolean
) : AbstractJvmType(name) {

    constructor(name: String, nullValue: String, size: Int, abbrevName: String = name) :
        this(name, nullValue, size, abbrevName, false)

    val box = JvmPrimitiveBoxType(name)

    override val packageName = "java.lang"

    override val nullable =
        if (isNullable)
            this
        else
            JvmPrimitiveType(className, nullValue, size, abbrevName, true)

}

/**
 * A `JvmPrimitiveBoxType` represents the boxed version of a primitive type.
 *
 * @param name the name of the boxed type to represent
 *
 * @since 1.0.0
 */
class JvmPrimitiveBoxType private constructor(
    name: String,
    override val isNullable: Boolean
) : AbstractJvmType(name) {

    constructor(name: String) : this(name, false)

    override val nullable =
        if (isNullable)
            this
        else
            JvmPrimitiveBoxType(className, true)

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