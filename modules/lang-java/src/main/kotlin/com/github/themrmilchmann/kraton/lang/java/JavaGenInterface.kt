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

import com.github.themrmilchmann.kraton.*
import java.io.*
import java.util.*

/**
 * Creates, registers and returns an object representing a Java interface.
 *
 * @receiver the profile of which the interface will be a member of
 *
 * @param fileName          the name for the interface
 * @param packageName       the name of the package for the interface
 * @param srcFolder         the name of the source folder for the interface
 * @param srcSet            the name of the source folder for the interface
 * @param documentation     the documentation for the interface
 * @param superInterfaces   the interfaces for the interface to extend
 * @param sorted            whether or not the interface's content will be
 *                          sorted
 *
 * @return the newly created and registered JavaInterface object
 *
 * @since 1.0.0
 */
fun Profile.javaInterface(
    fileName: String,
    packageName: String,
    srcFolder: String,
    srcSet: String,
    documentation: String? = null,
    superInterfaces: Array<out IJavaType>? = null,
    sorted: Boolean = false,
    copyrightHeader: String? = null,
    init: JavaInterface.() -> Unit
) = JavaInterface(fileName, packageName, documentation, superInterfaces, sorted, null, null)
    .also(init)
    .run { targetOf(this, packageName, srcFolder, srcSet, copyrightHeader) }

/**
 * Creates, registers and returns an object representing a Java interface.
 *
 * @receiver the enclosing type of which the interface will be a member of
 *
 * @param className         the name for the interface
 * @param documentation     the documentation for the interface
 * @param superInterfaces   the interfaces for the interface to extend
 * @param sorted            whether or not the interface's content will be
 *                          sorted
 * @param category          the category under which this interface will be
 *                          registered in the enclosing type
 *
 * @return the newly created and registered JavaInterface object
 *
 * @since 1.0.0
 */
fun JavaClass.javaInterface(
    className: String,
    documentation: String? = null,
    superInterfaces: Array<out IJavaType>? = null,
    sorted: Boolean = false,
    category: String? = null,
    init: JavaInterface.() -> Unit
) = JavaInterface(className, this.packageName, documentation, superInterfaces, sorted, category, this)
    .apply(init)
    .also { members.add(it) }

/**
 * Creates, registers and returns an object representing a Java interface.
 *
 * @receiver the enclosing type of which the interface will be a member of
 *
 * @param className         the name for the interface
 * @param documentation     the documentation for the interface
 * @param superInterfaces   the interfaces for the interface to extend
 * @param sorted            whether or not the interface's content will be
 *                          sorted
 * @param category          the category under which this interface will be
 *                          registered in the enclosing type
 *
 * @return the newly created and registered JavaInterface object
 *
 * @since 1.0.0
 */
fun JavaInterface.javaInterface(
    className: String,
    documentation: String? = null,
    superInterfaces: Array<out IJavaType>? = null,
    sorted: Boolean = false,
    category: String? = null,
    init: JavaInterface.() -> Unit
) = JavaInterface(className, this.packageName, documentation, superInterfaces, sorted, category, this)
    .apply(init)
    .also { members.add(it) }

/**
 * An object representing a Java interface.
 *
 * @property superInterfaces the interfaces extended by this interface
 *
 * @since 1.0.0
 */
class JavaInterface internal constructor(
    className: String,
    packageName: String,
    documentation: String?,
    val superInterfaces: Array<out IJavaType>?,
    sorted: Boolean,
    override val category: String?,
    containerType: JavaTopLevelType?
): JavaTopLevelType(className, packageName, documentation, sorted, containerType) {

    override val name: String
        get() = className

    override val weight: Int = WEIGHT_TOPLEVEL

    init {
        modifiers.forEach { it.value.applyImports.invoke(this) }
        superInterfaces?.forEach { import(it) }
    }

    /**
     * Creates, registers and returns an object representing a Java field.
     *
     * @receiver the type for the field
     *
     * @param name          the name for the field
     * @param documentation the documentation for the field
     * @param since         the documentation for the field's `@since` tag
     * @param category      the category under which the field will be
     *                      generated within the interface
     * @param see           the references to be in the documentation of the
     *                      field
     *
     * @return the newly created and registered JavaField object
     *
     * @since 1.0.0
     */
    operator fun IJavaType.invoke(
        name: String,
        value: String?,
        documentation: String,
        since: String? = null,
        category: String? = null,
        see: Array<out String>? = null
    ) = JavaField(this, arrayOf(name to value), documentation, since, category, see)
        .also {
            modifiers.forEach { it.value.applyImports.invoke(this@JavaInterface) }
            import(this)
            members.add(it)
        }

    /**
     * Creates, registers and returns an object representing one or multiple
     * Java field/s.
     *
     * This function may be used to generate multiple fields of the same type.
     * If this is the case the returned JavaField object will also not only
     * represent one of the fields but all of them.
     *
     * All fields registered by this function will be marked to be left
     * uninitialized.
     *
     * @receiver the type for the field/s
     *
     * @param names         the name/s for the field/s
     * @param documentation the documentation for the field/s
     * @param since         the documentation for the field's/fields' `@since`
     *                      tag
     * @param category      the category under which the field/s will be
     *                      generated within the interface
     * @param see           the references to be in the documentation of the
     *                      field/s
     *
     * @return the newly created and registered JavaField object
     *
     * @since 1.0.0
     */
    operator fun IJavaType.invoke(
        names: Array<String>,
        documentation: String,
        since: String? = null,
        category: String? = null,
        see: Array<out String>? = null
    ) = JavaField(this, names.map { it to null as String? }.toTypedArray(), documentation, since, category, see)
        .also {
            modifiers.forEach { it.value.applyImports.invoke(this@JavaInterface) }
            import(this)
            members.add(it)
        }

    /**
     * Creates, registers and returns an object representing one or multiple
     * Java field/s.
     *
     * This function may be used to generate multiple fields of the same type.
     * If this is the case the returned JavaField object will also not only
     * represent one of the fields but all of them.
     *
     * A value may be `null` to leave the field uninitialized.
     *
     * @receiver the type for the field/s
     *
     * @param entries       the name and value pairs for the field/s (A `null`
     *                      value will leave the respective field uninitialized.
     *                      For `null` use `"null"` instead.)
     * @param documentation the documentation for the field/s
     * @param since         the documentation for the field's/fields' `@since`
     *                      tag
     * @param category      the category under which the field/s will be
     *                      generated within the interface
     * @param see           the references to be in the documentation of the
     *                      field/s
     *
     * @return the newly created and registered JavaField object
     *
     * @since 1.0.0
     */
    operator fun IJavaType.invoke(
        vararg entries: Pair<String, String?>,
        documentation: String,
        since: String? = null,
        category: String? = null,
        see: Array<out String>? = null
    ) = JavaField(this, entries, documentation, since, category, see)
        .also {
            modifiers.forEach { it.value.applyImports.invoke(this@JavaInterface) }
            import(this)
            members.add(it)
        }

    /**
     * Creates, registers and returns an object representing a Java method.
     *
     * @receiver the return type for the method
     *
     * @param name              the name for the method
     * @param documentation     the documentation for the method
     * @param parameters        the parameters for the method
     * @param returnDoc         the documentation for the method's `@returnDoc`
     *                          tag
     * @param since             the documentation for the method's `@since` tag
     * @param category          the category under which this method will be
     *                          generated within the interface
     * @param exceptions        the exceptions that may be thrown by the method
     *                          and their respective documentation entries
     * @param see               the objects to be referenced in the method's
     *                          documentation
     * @param typeParameters    the type parameters for the method
     * @param body              the body for the method
     *
     * @return the newly created and registered JavaMethod object
     *
     * @since 1.0.0
     */
    operator fun IJavaType.invoke(
        name: String,
        documentation: String,
        vararg parameters: JavaParameter,
        returnDoc: String? = null,
        since: String? = null,
        category: String? = null,
        exceptions: Array<out Pair<IJavaType, String?>>? = null,
        see: Array<out String>? = null,
        typeParameters: Array<out Pair<JavaGenericType, String?>>? = null,
        body: String? = null
    ) = JavaMethod(this, name, documentation, parameters, returnDoc, since, category, exceptions, see, typeParameters, body)
        .also {
            modifiers.forEach { it.value.applyImports.invoke(this@JavaInterface) }
            import(this)
            parameters.forEach { import(it.type) }
            exceptions?.forEach { import(it.first) }
            typeParameters?.forEach { import(it.first) }
            members.add(it)
        }

    /**
     * Creates, registers and returns an object representing a Java type
     * parameter.
     *
     * @receiver the name for the type parameter
     *
     * @param documentation the documentation for the type parameter
     * @param bounds the bounds for the type parameter
     *
     * @return the newly created and registered JavaGenericType object
     *
     * @since 1.0.0
     */
    fun String.typeParameter(
        documentation: String? = null,
        vararg bounds: IJavaType
    ) = JavaGenericType(this, *bounds)
        .also {
            bounds.forEach { import(it) }
            typeParameters.add(it to documentation)
        }

    override fun PrintWriter.printTypeDeclaration() {
        print("class ")
        print(className)

        if (typeParameters.isNotEmpty()) {
            print("<")
            print(StringJoiner(", ").apply {
                typeParameters.forEach { add(it.toString()) }
            })
            print(">")
        }

        if (superInterfaces != null) {
            print(" implements ")
            print(StringJoiner(", ").apply {
                superInterfaces.forEach { add(it.toString()) }
            })
        }
    }

    override fun toQualifiedString() = className

}