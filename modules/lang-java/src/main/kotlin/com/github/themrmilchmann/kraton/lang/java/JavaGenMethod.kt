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

import java.io.*
import java.util.*

/**
 * A java method.
 *
 * @property returnType the return type of this method
 * @property name the name of this method
 * @property documentation the documentation of this method
 * @property parameters the parameters of this method
 * @property returnDoc the return documentation of this method
 * @property since the value of this `@since` blocktag for this method
 * @property category the category this method is a part of
 * @property exceptions the checked exceptions that may be thrown by this method
 * @property see the `@see` referenced objects for this method's documentation
 * @property typeParameters the type parameters of this method
 * @property body the body of this method
 * @constructor Creates a new JavaMethod with the given properties.
 *
 * @since 1.0.0
 */
open class JavaMethod internal constructor(
	val returnType: IJavaType,
	override val name: String,
	val documentation: String,
	val parameters: Array<out JavaParameter>,
	val returnDoc: String?,
	val since: String?,
	override val category: String?,
	val exceptions: Array<out Pair<IJavaType, String?>>?,
	val see: Array<out String>?,
	val typeParameters: Array<out Pair<JavaGenericType, String?>>?,
	private val body: String?
): JavaModifierTarget(), JavaBodyMember {

	override val weight: Int
		get() = if (has(static)) WEIGHT_STATIC_METHOD else WEIGHT_INSTANCE_METHOD

	override fun PrintWriter.printMember(indent: String): Boolean {
		val documentation = toJavaDoc(indent)
		if (documentation != null) println(documentation)

		printAnnotations(indent)
		print(indent)
		printModifiers()
		printMethodHead()
		print("(")

		if (parameters.isNotEmpty()) {
			print(StringJoiner(", ").apply {
				parameters.forEach {
					add("${printAnnotationsInline()}${it.type} ${it.name}")
				}
			})
		}

		print(")")

		if (body == null) {
			print(";")
		} else {
			print(" {")

			if (body.isNotEmpty()) {
				var body: String = body

				while (body.startsWith(LN)) body = body.removePrefix(LN)
				while (body.endsWith(LN)) body = body.removeSuffix(LN)

				println()
				body.lineSequence().forEach {
					print(indent + INDENT)
					println(it)
				}

				print(indent)
			}

			println("}")
		}

		println()

		return true
	}

    /**
     * Prints the head of this method.
     *
     * @receiver the PrintWriter to print to
     *
     * @since 1.0.0
     */
	internal open fun PrintWriter.printMethodHead() {
		print(returnType.toString())
		print(" ")
		print(name)
	}

}

/**
 * A java constructor.
 *
 * @constructor Creates a new JavaConstructor with the given properties.
 *
 * @since 1.0.0
 */
class JavaConstructor internal constructor(
	returnType: IJavaType,
	documentation: String,
	parameters: Array<out JavaParameter>,
	returnDoc: String?,
	since: String?,
	category: String?,
	exceptions: Array<out Pair<IJavaType, String?>>?,
	see: Array<out String>?,
	typeParameters: Array<out Pair<JavaGenericType, String?>>?,
	body: String?
): JavaMethod(returnType, returnType.toString(), documentation, parameters, returnDoc, since, category, exceptions, see, typeParameters, body) {

    /**
     * Prints the head of this constructor.
     *
     * @receiver the PrintWriter to print to
     *
     * @since 1.0.0
     */
	override fun PrintWriter.printMethodHead() {
		print(returnType.toString())
	}

}