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
 * Registers a new top-level interface with the given properties.
 *
 * @receiver the profile in which the interface will be registered
 * @param fileName the name for the new interface
 * @param packageName the name of the package for the new interface
 * @param srcFolder the name of the source folder for the new interface
 * @param srcSet the name of the source folder for the new interface
 * @param documentation the documentation for the new interface
 * @param superInterfaces the super interfaces for the new interface
 * @param sorted whether or not the content the interface's content will be sorted
 * @param category the category under which this call will be registered in the receiver
 * @return the newly registered interface
 *
 * @since 1.0.0
 */
fun Profile.javaInterface(
	fileName: String,
	packageName: String,
	srcFolder: String,
	srcSet: String,
	documentation: String? = null,
	superInterfaces: Array<out IJavaType>?,
	sorted: Boolean = false,
	category: String? = null,
    copyrightHeader: String? = null,
	init: JavaInterface.() -> Unit
) = JavaInterface(fileName, packageName, documentation, superInterfaces, sorted, category)
	.also(init)
	.run { targetOf(this, packageName, srcFolder, srcSet, copyrightHeader) }

/**
 * Registers a new interface with the given properties as a subinterface of the receiver class.
 *
 * @receiver the enclosing class
 * @param className the name for the new interface
 * @param documentation the documentation for the new interface
 * @param superInterfaces the super interfaces for the new interface
 * @param sorted whether or not the content the interface's content will be sorted
 * @param category the category under which this call will be registered in the receiver
 * @return the newly registered interface
 *
 * @since 1.0.0
 */
fun JavaClass.javaInterface(
	className: String,
	documentation: String? = null,
	superInterfaces: Array<out IJavaType>?,
	sorted: Boolean = false,
	category: String? = null,
	init: JavaInterface.() -> Unit
) = JavaInterface(className, this.packageName, documentation, superInterfaces, sorted, category)
	.apply(init)
	.also { members.add(it) }

/**
 * Registers a new interface with the given properties as a subinterface of the receiver interface.
 *
 * @receiver the enclosing interface
 * @param className the name for the new interface
 * @param documentation the documentation for the new interface
 * @param superInterfaces the super interfaces for the new interface
 * @param sorted whether or not the content the interface's content will be sorted
 * @param category the category under which this call will be registered in the receiver
 * @return the newly registered interface
 *
 * @since 1.0.0
 */
fun JavaInterface.javaInterface(
	className: String,
	documentation: String? = null,
	superInterfaces: Array<out IJavaType>?,
	sorted: Boolean = false,
	category: String? = null,
	init: JavaInterface.() -> Unit
) = JavaInterface(className, this.packageName, documentation, superInterfaces, sorted, category)
	.apply(init)
	.also { members.add(it) }

/**
 * A java interface.
 *
 * @property superInterfaces the super interfaces of this interface
 *
 * @since 1.0.0
 */
class JavaInterface internal constructor(
	className: String,
	packageName: String,
	documentation: String?,
	val superInterfaces: Array<out IJavaType>?,
	sorted: Boolean,
	override val category: String?
): JavaTopLevelType(className, packageName, documentation, sorted) {

	override val name: String
		get() = className

	override val weight: Int = WEIGHT_TOPLEVEL

    /**
     * Creates, registers and returns a new object representing a Java field.
     *
     * @receiver the type for the field
     * @param name the names for the field
     * @param documentation the documentation for the field
     * @param since the documentation for the field's `@since` tag
     * @param category the category under which this field will be generated within the interface
     * @param see the references in the documentation for the field
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
        .also { members.add(it) }

    /**
     * Creates, registers and returns a new object representing a Java field.
     *
     * @receiver the type for the set of fields
     * @param names the names for the fields
     * @param documentation the documentation for the field
     * @param since the documentation for the field's `@since` tag
     * @param category the category under which this field will be generated within the interface
     * @param see the references in the documentation for the field
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
        .also { members.add(it) }

    /**
     * Creates and registers a set of fields with the given names and values.
     *
     * A value may be `null` to leave the field uninitialized.
     *
     * @receiver the type of this set of fields
     * @param entries the name and value pairs of the fields
     * @param documentation the documentation for the field
     * @param since the documentation for the field's `@since` tag
     * @param category the category under which this field will be generated within the interface
     * @param see the references in the documentation for the field
     * @return the newly registered field
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
        .also { members.add(it) }

    /**
     * Creates, registers and returns a new object representing a Java method.
     *
     * @receiver the class to which the method will be registered
     * @param name the name for the method
     * @param documentation the documentation for the method
     * @param parameters the parameters for the method
     * @param returnDoc the documentation for the method's `@returnDoc` tag
     * @param since the documentation for the method's `@since` tag
     * @param category the category under which this method will be generated within the interface
     * @param exceptions the exceptions that may be thrown by the method and their respective documentation entries
     * @param see the objects to be referenced in the method's documentation
     * @param typeParameters the type parameters for the method
     * @param body the body for the method
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
        .also { members.add(it) }

    /**
     * Creates, registers and returns a new type parameter.
     *
     * @receiver the name for the type parameter
     * @param documentation the documentation for the type parameter
     * @param bounds the bounds for the type parameter
     * @return the newly registered type parameter reference
     *
     * @since 1.0.0
     */
    fun String.typeParameter(
        documentation: String? = null,
        vararg bounds: IJavaType
    ) = JavaGenericType(this, *bounds)
        .also { typeParameters.add(it to documentation) }

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