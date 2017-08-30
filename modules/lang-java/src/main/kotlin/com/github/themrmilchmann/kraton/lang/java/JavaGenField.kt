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
 * An object representing one or multiple Java field/s.
 *
 * @property type           the type of this field/s
 * @property entries        the key-value mappings of the field/s (A `null`
 *                          value will leave the respective field
 *                          uninitialized. For `null` use `"null"` instead.)
 * @property documentation  the documentation of the field/s
 * @property since          the documentation of the field's/fields' `@since`
 *                          tag
 * @property see            the references to be in the documentation of the
 *                          field/s
 *
 * @since 1.0.0
 */
class JavaField internal constructor(
	val type: IJavaType,
	val entries: Array<out Pair<String, String?>>,
	val documentation: String,
    val since: String?,
	override val category: String?,
    val see: Array<out String>?
): JavaModifierTarget(), JavaBodyMember {

	override val name = entries.first().first

	override val weight: Int
		get() = if (has(static))
			if (has(final))
				WEIGHT_CONSTANT_FIELD
			else
				WEIGHT_STATIC_FIELD
		else
			WEIGHT_INSTANCE_FIELD

	override fun PrintWriter.printMember(indent: String): Boolean {
		val documentation = documentation.toJavaDoc(indent, see = see, since = since)
		if (documentation != null) println(documentation)

		printAnnotations(indent)
		print(indent)
		printModifiers()
		print(type.toString())

		if (entries.size == 1) {
			print(" ")
			print(entries.first().first)

			if (entries.first().second != null) print(" = ${entries.first().second}")
		} else {
			println()

			val entryIndent = indent + indent

			print(entryIndent)
			print(StringJoiner(",$LN$entryIndent").run {
				entries.forEach {
					add("${it.first}${if (it.second != null) " = ${it.second}" else ""}")
				}
				toString()
			})
		}

		println(";$LN")

		return false
	}

}