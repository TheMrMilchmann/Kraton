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
import com.github.themrmilchmann.kraton.lang.jvm.*
import java.io.*
import java.util.*

/**
 * Creates, registers and returns an object representing a Java class.
 *
 * @receiver the profile of which the class will be a member of
 *
 * @param fileName      the name for the class
 * @param packageName   the name of the package for the class
 * @param srcFolder     the name of the source folder for the class
 * @param srcSet        the name of the source folder for the class
 * @param documentation the documentation for the class
 * @param since         the value for the class' `@since` tag
 * @param superClass    the parent class for the class
 * @param interfaces    the interfaces for the class to implement
 * @param sorted        whether or not the class' content will be sorted
 *
 * @return the newly created and registered JavaClass object
 *
 * @since 1.0.0
 */
@JvmOverloads
fun Profile.javaClass(
    fileName: String,
    packageName: String,
    srcFolder: String,
    srcSet: String,
    documentation: String? = null,
    since: String? = null,
    superClass: IJvmType? = null,
    interfaces: Array<out IJvmType>? = null,
    sorted: Boolean = false,
    copyrightHeader: String? = null,
    init: JavaClass.() -> Unit
) = JavaClass(fileName, packageName, documentation, since, superClass, interfaces, sorted, null, null)
    .apply { import("java.lang", false, true) }
    .also(init)
    .run { targetOf(this, packageName, srcFolder, srcSet, copyrightHeader) }

/**
 * Creates, registers and returns an object representing a Java class.
 *
 * @receiver the enclosing type of which the class will be a member of
 *
 * @param className     the name for the class
 * @param documentation the documentation for the class
 * @param since         the value for the class' `@since` tag
 * @param superClass    the parent class for the class
 * @param interfaces    the interfaces for the class to implement
 * @param sorted        whether or not the class' content will be sorted
 * @param category      the category under which this class will be registered
 *                      in the enclosing type
 *
 * @return the newly created and registered JavaClass object
 *
 * @since 1.0.0
 */
@JvmOverloads
fun JavaClass.javaClass(
    className: String,
    documentation: String? = null,
    since: String? = null,
    superClass: IJvmType? = null,
    interfaces: Array<out IJvmType>? = null,
    sorted: Boolean = false,
    category: String? = null,
    init: JavaClass.() -> Unit
) = JavaClass(className, this.packageName, documentation, since, superClass, interfaces, sorted, category, this)
    .apply(init)
    .also { members.add(it) }

/**
 * Creates, registers and returns an object representing a Java class.
 *
 * @receiver the enclosing type of which the class will be a member of
 *
 * @param className     the name for the class
 * @param documentation the documentation for the class
 * @param since         the value for the class' `@since` tag
 * @param superClass    the parent class for the class
 * @param interfaces    the interfaces for the class to implement
 * @param sorted        whether or not the class' content will be sorted
 * @param category      the category under which this class will be registered
 *                      in the enclosing type
 *
 * @return the newly created and registered JavaClass object
 *
 * @since 1.0.0
 */
@JvmOverloads
fun JavaInterface.javaClass(
    className: String,
    documentation: String? = null,
    since: String? = null,
    superClass: IJvmType? = null,
    interfaces: Array<out IJvmType>? = null,
    sorted: Boolean = false,
    category: String? = null,
    init: JavaClass.() -> Unit
) = JavaClass(className, this.packageName, documentation, since, superClass, interfaces, sorted, category, this)
    .apply(init)
    .also { members.add(it) }

/**
 * An object representing a Java class.
 *
 * @property superClass the parent class of this class
 * @property interfaces the interfaces implemented by this class
 *
 * @since 1.0.0
 */
class JavaClass internal constructor(
    className: String,
    packageName: String,
    documentation: String?,
    since: String?,
    val superClass: IJvmType?,
    val interfaces: Array<out IJvmType>?,
    sorted: Boolean,
    override val category: String?,
    containerType: JavaTopLevelType?
): JavaTopLevelType(className, packageName, documentation, since, sorted, containerType) {

    override val name: String
        get() = className

    override val weight: Int = WEIGHT_TOPLEVEL

    init {
        modifiers.forEach { it.value.applyImports.invoke(this) }
        if (superClass != null) import(superClass)
        interfaces?.forEach { import(it) }
    }

    /**
     * Creates, registers and returns an object representing a Java constructor.
     *
     * @param documentation     the documentation for the constructor
     * @param parameters        the parameters for the constructor
     * @param returnDoc         the documentation for the constructor's
     *                          `@returnDoc` tag
     * @param since             the documentation for the constructor's `@since`
     *                          tag
     * @param category          the category under which this constructor will
     *                          be generated within the class
     * @param exceptions        the exceptions that may be thrown by the
     *                          constructor and their respective documentation
     *                          entries
     * @param see               the objects to be referenced in the
     *                          constructor's documentation
     * @param typeParameters    the type parameters for the constructor
     * @param body              the body for the constructor
     *
     * @return the newly created and registered JavaConstructor object
     *
     * @since 1.0.0
     */
    @JvmOverloads
    fun constructor(
        documentation: String,
        vararg parameters: JavaParameter,
        returnDoc: String? = null,
        since: String? = null,
        category: String? = null,
        exceptions: Array<out Pair<IJvmType, String?>>? = null,
        see: Array<out String>? = null,
        typeParameters: Array<out Pair<JvmGenericType, String?>>? = null,
        body: String? = null
    ) = JavaConstructor(this, documentation, parameters, returnDoc, since, category, exceptions, see, typeParameters, body)
        .also {
            modifiers.forEach { it.value.applyImports.invoke(this@JavaClass) }
            parameters.forEach { import(it.type) }
            exceptions?.forEach { import(it.first) }
            typeParameters?.forEach { import(it.first) }
            members.add(it)
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
     *                      generated within the class
     * @param see           the references to be in the documentation of the
     *                      field
     *
     * @return the newly created and registered JavaField object
     *
     * @since 1.0.0
     */
    @JvmOverloads
    operator fun IJvmType.invoke(
        name: String,
        value: String?,
        documentation: String,
        since: String? = null,
        category: String? = null,
        see: List<String>? = null
    ) = JavaField(this, arrayOf(name to value), documentation, since, category, see)
        .also {
            modifiers.forEach { it.value.applyImports.invoke(this@JavaClass) }
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
     *                      generated within the class
     * @param see           the references to be in the documentation of the
     *                      field/s
     *
     * @return the newly created and registered JavaField object
     *
     * @since 1.0.0
     */
    @JvmOverloads
    operator fun IJvmType.invoke(
        names: Array<String>,
        documentation: String,
        since: String? = null,
        category: String? = null,
        see: List<String>? = null
    ) = JavaField(this, names.map { it to null as String? }.toTypedArray(), documentation, since, category, see)
        .also {
            modifiers.forEach { it.value.applyImports.invoke(this@JavaClass) }
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
     *                      generated within the class
     * @param see           the references to be in the documentation of the
     *                      field/s
     *
     * @return the newly created and registered JavaField object
     *
     * @since 1.0.0
     */
    @JvmOverloads
    operator fun IJvmType.invoke(
        vararg entries: Pair<String, String?>,
        documentation: String,
        since: String? = null,
        category: String? = null,
        see: List<String>? = null
    ) = JavaField(this, entries, documentation, since, category, see)
        .also {
            modifiers.forEach { it.value.applyImports.invoke(this@JavaClass) }
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
     *                          generated within the class
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
    @JvmOverloads
    operator fun IJvmType.invoke(
        name: String,
        documentation: String,
        vararg parameters: JavaParameter,
        returnDoc: String? = null,
        since: String? = null,
        category: String? = null,
        exceptions: Array<out Pair<IJvmType, String?>>? = null,
        see: Array<out String>? = null,
        typeParameters: Array<out Pair<JvmGenericType, String?>>? = null,
        body: String? = null
    ) = JavaMethod(this, name, documentation, parameters, returnDoc, since, category, exceptions, see, typeParameters, body)
        .also {
            modifiers.forEach { it.value.applyImports.invoke(this@JavaClass) }
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
     * @param bounds        the bounds for the type parameter
     *
     * @return the newly created and registered JavaGenericType object
     *
     * @since 1.0.0
     */
    @JvmOverloads
    fun String.typeParameter(
        documentation: String? = null,
        vararg bounds: IJvmType
    ) = JvmGenericType(this, *bounds)
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
                typeParameters.forEach { add(it.first.asString(this@JavaClass)) }
            })
            print(">")
        }

        if (superClass != null) {
            print(" extends ")
            print(superClass.asString(this@JavaClass))
        }

        if (interfaces != null) {
            print(" implements ")
            print(StringJoiner(", ").apply {
                interfaces.forEach { add(it.asString(this@JavaClass)) }
            })
        }
    }

}