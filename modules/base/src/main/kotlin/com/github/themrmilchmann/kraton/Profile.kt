/*
 * Original work Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 *
 * Modified work Copyright (c) 2017 Leon Linhart,
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
package com.github.themrmilchmann.kraton

import java.io.*
import java.nio.file.*

/**
 * # Profile
 * A Profile is a top level type that GeneratorTarget's should be registered to.
 *
 * @since 1.0.0
 */
class Profile(
    init: Profile.() -> Unit
) {

    /**
     * The list of targets managed by this profile.
     *
     * @since 1.0.0
     */
    val targets = ArrayList<GeneratorTarget>()

    init {
        init.invoke(this)
    }

}

/**
 * # GeneratorTarget
 * A GeneratorTarget defines a file template for the generator to process.
 * Usually end user's shouldn't need to call this class directly but use functionality of their respective language module instead.
 *
 * ## Output location
 * The Generator is built to assume that the project is using a directory layout that is similar to Kraton's. However, this behaviour is fully customizable and
 * may be altered.
 *
 * The path of the file is constructed as follows:
 * `srcFolder/srcSet/language/packageName/fileName.appendix`
 *
 * @property fileName the name for the file
 * @property language the language to be used
 * @property packageName the package in which the generated output is to be placed
 * @property srcFolder the source folder for the generated output
 * @property srcSet the source set for the generated output
 * @property appendix the file ending (defaults to the language)
 * @constructor Creates a new GeneratorTarget.
 *
 * @since 1.0.0
 */
abstract class GeneratorTarget(
    internal val fileName: String,
    internal val language: String,
    internal val packageName: String,
    internal val srcFolder: String,
    internal val srcSet: String,
    internal val appendix: String = language
) {

	private fun getSourceFileName(): String? {
		// Nasty hack to retrieve the source file that defines this template, without having to specify it explicitly. This enables incremental builds to work
		// even with arbitrary file names or when multiple templates are bundled in the same file.
		try {
			throw RuntimeException()
		} catch (t: Throwable) {
			return t.stackTrace.asSequence()
				.filter { !it.className.startsWith("com.github.themrmilchmann.kraton.") || it.className.startsWith("com.github.themrmilchmann.kraton.test.") }
				.mapNotNull { it.className.substring(0, it.className.lastIndexOf('.') + 1).replace('.', '/') + it.fileName }
				.filter { it.endsWith(".kt") }
				.firstOrNull()
		}
	}

	private val sourceFile = getSourceFileName()
	internal open fun getLastModified(root: String): Long = Paths.get(root, sourceFile).let {
		if (Files.isRegularFile(it)) it else
			throw IllegalStateException("The source file for template $packageName.$fileName does not exist ($it).")
	}.lastModified

    /**
     * Prints this GeneratorTarget.
     *
     * @receiver the PrintWriter to print to
     *
     * @since 1.0.0
     */
	abstract fun PrintWriter.printTarget()

}