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
import kotlin.collections.LinkedHashSet

private val CATEGORY = "(\\d+)\\Q_\\E(.*)".toRegex()

internal const val CATEGORY_DIVIDER = "################################################################################################################################################################"
internal const val INDENT = "    "
internal const val LN = "\n"

internal const val WEIGHT_CONSTANT_FIELD = 0
internal const val WEIGHT_STATIC_FIELD = 1
internal const val WEIGHT_STATIC_METHOD = 2
internal const val WEIGHT_INSTANCE_FIELD = 3
internal const val WEIGHT_INSTANCE_METHOD = 5
internal const val WEIGHT_TOPLEVEL = Int.MAX_VALUE

internal fun <T: JavaTopLevelType> Profile.targetOf(type: T, packageName: String, srcFolder: String, srcSet: String, copyrightHeader: String?) =
	JavaGeneratorTarget(type.className, packageName, srcFolder, srcSet, {
        if (copyrightHeader != null) println(copyrightHeader)
		print("package ")
		print(packageName)
		println(";")
		println()

		var specialImportId = ""

		if (type.imports.any()) {
			type.imports.forEach {
				if (specialImportId.isNotEmpty() && !it.packageName.startsWith(specialImportId)) println()

				specialImportId = when (it.packageName.startsWith("java.")) {
					true -> "java."
					false -> when (it.packageName.startsWith("javax.")) {
						true -> "javax."
						false -> ""
					}
				}

				it.apply { printImport() }
			}

			println()
		}

		type.apply { printType("") }
	}).run {
		targets.add(this)
		type
	}

internal class JavaGeneratorTarget(
	fileName: String,
	packageName: String,
	srcFolder: String,
	srcSet: String,
	private val print: PrintWriter.() -> Unit
): GeneratorTarget(fileName, "java", packageName, srcFolder, srcSet) {

	override fun PrintWriter.printTarget() {
		this.apply(print)
	}

}

/**
 * A java top-level type.
 *
 * @property className the name of this type
 * @property packageName the name of the package containing this type
 * @property documentation the documentation of this type
 * @constructor Creates a new java top-level type.
 *
 * @since 1.0.0
 */
abstract class JavaTopLevelType(
	val className: String,
	val packageName: String,
	val documentation: String?,
	sorted: Boolean
): JavaModifierTarget(), JavaBodyMember, IJavaType {

	internal val imports = TreeSet<JavaImport>()
	internal val members: MutableSet<JavaBodyMember> = if (sorted) TreeSet() else LinkedHashSet()
	internal val typeParameters = mutableListOf<Pair<JavaGenericType, String?>>()

	internal fun PrintWriter.printType(indent: String) {
		val documentation = documentation.toJavaDoc(indent)
		if (documentation != null) println(documentation)

		print(indent)
		printAnnotations("$LN$indent")
		printModifiers()
		printTypeDeclaration()
		print(" {")

		if (members.isNotEmpty()) {
			println(LN)

			val subIndent = indent + INDENT
			var isFirst = true
			var prevCategory: String? = null
			var isLastLineBlank = true

			members.forEach {
				val cat = it.category

				if (cat != null) {
					val mCat = CATEGORY.matchEntire(cat) ?: throw IllegalArgumentException("Category name does not match pattern")
					val category = mCat.groupValues[2]

					if (cat != prevCategory && !(isFirst && category.isEmpty())) {
						if (category.isNotEmpty()) {
							if (!isLastLineBlank) println()

							println("$subIndent// ${CATEGORY_DIVIDER.substring(subIndent.length + 3)}")
							println("$subIndent// # $category ${CATEGORY_DIVIDER.substring(subIndent.length + category.length + 6)}")
							println("$subIndent// ${CATEGORY_DIVIDER.substring(subIndent.length + 3)}")
						}

						println()
					}

					isFirst = false
					prevCategory = cat
				}

				it.run { isLastLineBlank = printMember(subIndent) }
			}

			print(indent)
		}

		print("}")
	}

    /**
     * Prints the declaration of this top-level type.
     *
     * @receiver the PrintWriter to print to
     *
     * @since 1.0.0
     */
	internal abstract fun PrintWriter.printTypeDeclaration()

	override fun PrintWriter.printMember(indent: String): Boolean {
		printType(indent)
		println(LN)

		return false
	}

	override fun toPackageString() = packageName
	override fun toString() = className

}

internal interface JavaBodyMember : Comparable<JavaBodyMember> {

    /**
     * The name of this java object.
     *
     * @since 1.0.0
     */
	val name: String

    /**
     * The weight used to sort this java object.
     *
     * @since 1.0.0
     */
    val weight: Int

    /**
     * The category declaration for this object.
     *
     * @since 1.0.0
     */
    val category: String?

	override fun compareTo(other: JavaBodyMember) = when(this.weight.compareTo(other.weight)) {
		-1 -> -1
		1 -> 1
		else -> when (name.compareTo(other.name)) {
			-1 -> -1
			else -> 1
		}
	}

    /**
     * Prints this java object to the given PrintWriter.
     *
     * @receiver the receiver to which this member will be printed
     * @param indent the indent to be used
     * @return returns whether or not this member ends with a blank line
     *
     * @since 1.0.0
     */
	fun PrintWriter.printMember(indent: String): Boolean

}

internal open class JavaImport(
	val packageName: String,
	val typeQualifier: String
): Comparable<JavaImport> {

	constructor(type: IJavaType): this(type.toPackageString()!!, type.toQualifiedString())

	override fun compareTo(other: JavaImport): Int {
		if (this is JavaStaticImport) {
			if (other !is JavaStaticImport)
				return 1
		} else if (other is JavaStaticImport)
			return -1

		if (packageName.startsWith("java.") && !other.packageName.startsWith("java.")) return -1
		if (!packageName.startsWith("java.") && other.packageName.startsWith("java.")) return 1
		if (packageName.startsWith("javax.") && !other.packageName.startsWith("javax.")) return -1
		if (!packageName.startsWith("javax.") && other.packageName.startsWith("javax.")) return 1

		val cmp = packageName.compareTo(other.packageName)
		if (cmp != 0) return cmp

		if (typeQualifier == "*" || other.typeQualifier == "*") return 0

		return typeQualifier.compareTo(other.typeQualifier)
	}

	fun PrintWriter.printImport() = println(this@JavaImport)

	override fun toString() = "import $packageName.$typeQualifier"

}

internal class JavaStaticImport(
	packageName: String
): JavaImport(packageName, "*") {

	override fun toString() = "import static $packageName.$typeQualifier"

}