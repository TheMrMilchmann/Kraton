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
import java.lang.reflect.*
import java.nio.*
import java.nio.file.*
import java.nio.file.attribute.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import java.util.function.*
import kotlin.streams.*
import kotlin.system.*

/**
 * The entry point of the generator.
 *
 * @since 1.0.0
 */
fun main(args: Array<String>) {
	cliOutln(CLI_HEADER)

	if (args.size < 2) {
		cliOutln(CLI_HELP)
		exitProcess(-1)
	}

	val validateDirectory: (String, String) -> String = { name: String, path: String ->
		if (!Files.isDirectory(Paths.get(path))) throw IllegalArgumentException("Invalid $name path: $path")
        path
	}

    val dTemplateSource = validateDirectory("template source", args[0])
    val dGenerationTarget = validateDirectory("generation target", args[1])
	var generator: String? = null
	var force = false

	val itr = args.iterator()
	itr.next()
	itr.next()

	while (itr.hasNext()) {
		val arg = itr.next()
		println(arg)

		val argName = arg.substring(if (arg.startsWith("--")) 2 else 1)

		when (argName) {
			"v", "version" -> {
				cliOut(CLI_HEADER)
				exitProcess(0)
			}
			"h", "help" -> {
				cliOut(CLI_HELP)
				exitProcess(0)
			}
			"f", "force" -> force = true
			"g", "generator" -> {
				if (!itr.hasNext()) throw IllegalArgumentException("-g must be followed by the generator source")

				generator = itr.next()
			}
		}
	}

    generate(dTemplateSource, dGenerationTarget, generator, force) {
		val pool = ForkJoinPool.commonPool()

        val relevantPackages = Files.find(Paths.get(dTemplateSource), Int.MAX_VALUE, BiPredicate { file, _ ->
            Files.isReadable(file) && Files.isDirectory(file) && Files.list(file).anyMatch { !Files.isDirectory(it) && KOTLIN_PATH_MATCHER.matches(it) }
        }).peek {
			val packageLastModified = it.lastModified(maxDepth = 1)
			packageLastModifiedMap[Paths.get(dTemplateSource).relativize(it).toString().replace("[\\\\/]".toRegex(), ".")] = packageLastModified
		}.sorted()
			.toList()

        try {
            val errors = AtomicInteger()

            CountDownLatch(relevantPackages.size).let { latch ->
                fun generate(packageName: String) {
                    pool.submit {
                        try {
							this.generate(packageName)
						} catch (t: Throwable) {
							errors.incrementAndGet()
							t.printStackTrace()
                        }

                        latch.countDown()
                    }
                }

                packageLastModifiedMap.keys.forEach { generate(it) }
                latch.await()
            }

            if (errors.get() != 0) throw RuntimeException("Generation failed")
		} finally {
			pool.shutdown()
		}
    }
}

private fun generate(
    srcPath: String,
    trgPath: String,
	generator: String?,
	force: Boolean,
    generate: Generator.() -> Unit
) {
    Generator(srcPath, trgPath, generator, force).generate()
}

private class Generator(
    val srcPath: String,
    val trgPath: String,
	generator: String?,
	val force: Boolean
) {

	private val GENERATOR_LAST_MODIFIED = if (generator != null) Paths.get(generator).lastModified else 0L

	private fun methodFilter(method: Method, javaClass: Class<*>) =
        // static
        method.modifiers and Modifier.STATIC != 0 &&
            // returns NativeClass
            method.returnType === javaClass &&
            // has no arguments
            method.parameterTypes.isEmpty()

	private fun apply(packagePath: String, packageName: String, consume: Sequence<Method>.() -> Unit) {
		val packageDirectory = Paths.get(packagePath)
		if (!Files.isDirectory(packageDirectory)) throw IllegalStateException()

		Files.list(packageDirectory)
			.filter { KOTLIN_PATH_MATCHER.matches(it) }
			.sorted()
			.also {
				it.forEach {
					try {
						Class
							.forName("$packageName.${it.fileName.toString().substringBeforeLast('.').upperCaseFirst}Kt")
							.methods
							.asSequence()
							.consume()
					} catch (e: ClassNotFoundException) {
						e.printStackTrace()
						// ignore
					}
				}
				it.close()
			}
	}

	internal fun generate(packageName: String) {
		val packagePath = "$srcPath/${packageName.replace('.', '/')}"
		val packageLastModified = Paths.get(packagePath).lastModified(maxDepth = 1)
		packageLastModifiedMap[packageName] = packageLastModified

		// Find the template methods
		val templates = TreeSet<Method> { alpha, beta -> alpha.name.compareTo(beta.name) }
		apply(packagePath, packageName) {
			filterTo(templates) {
				methodFilter(it, Profile::class.java)
			}
		}

		if (templates.isEmpty()) {
			println("*WARNING* No templates found in $packageName package.")
			return
		}

		val targets = mutableListOf<GeneratorTarget>()

		for (template in templates) {
			val profile = template.invoke(null) as Profile? ?: continue
			targets.addAll(profile.targets)
		}

		targets.forEach { generate(it, maxOf(packageLastModified, GENERATOR_LAST_MODIFIED)) }
	}

    private fun generate(target: GeneratorTarget, packageLastModified: Long) {
		val packagePath = target.packageName.replace('.', '/')
		val output = Paths.get(StringJoiner("/").run {             // Example:
			add(trgPath)                                                    // Z:/dev/project
			add(target.srcFolder)                                           // /modules/com.example.module/src
			if (target.srcSet.isNotEmpty()) add(target.srcSet)              // /main-generated
			add(target.language)                                            // /java
			add(packagePath)      											// /com/example/package
			add("${target.fileName}.${target.appendix}")                    // /Example.java
			toString()
		})

		val lmt = Math.max(target.getLastModified("$srcPath/"), packageLastModified)

		if (lmt < output.lastModified && !force) return

		generateOutput(target, output) { it.printTarget() }
    }

}

private val packageLastModifiedMap: MutableMap<String, Long> = ConcurrentHashMap()

internal val Path.lastModified
    get() = if (Files.isRegularFile(this))
        Files.getLastModifiedTime(this).toMillis()
    else
        0L

private val KOTLIN_PATH_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**/*.kt")

internal fun Path.lastModified(
    maxDepth: Int = Int.MAX_VALUE,
    glob: String? = null,
    matcher: PathMatcher = if (glob == null) KOTLIN_PATH_MATCHER else FileSystems.getDefault().getPathMatcher("glob:$glob")
): Long {
    if (!Files.isDirectory(this)) throw IllegalStateException()

    return Files.find(this, maxDepth, BiPredicate { path, _ -> matcher.matches(path) })
        .mapToLong(Path::lastModified)
        .reduce(0L, Math::max)
}

private fun ensurePath(path: Path) {
    val parent = path.parent ?: throw IllegalArgumentException("The given path has no parent directory.")

    if (!Files.isDirectory(parent)) {
        cliOutln("MKDIR: $parent")
        Files.createDirectories(parent)
    }
}

private fun readFile(file: Path) = Files.newByteChannel(file).use {
    val bytesTotal = it.size().toInt()
    val buffer = ByteBuffer.allocateDirect(bytesTotal)
    var bytesRead = 0

    do {
        bytesRead += it.read(buffer)
    } while (bytesRead < bytesTotal)

    buffer.flip()
    buffer
}

private class KratonWriter(out: Writer): PrintWriter(out) {

	/**
     * Terminates the current line by writing the line separator string. The line separator string is always a single newline character (<code>'\n'</code>).
	 */
	override fun println() = print("\n")

}

private fun <T> generateOutput(target: T, file: Path, lmt: Long? = null, generate: T.(PrintWriter) -> Unit) {
    ensurePath(file)

    if (Files.isRegularFile(file)) {
        // Generate in memory
        val baos = ByteArrayOutputStream(4 * 1024)
		KratonWriter(OutputStreamWriter(baos, Charsets.UTF_8)).use { target.generate(it) }

        // Compare the existing file content with the generated content.
        val before = readFile(file)
        val after = baos.toByteArray()

        fun somethingChanged(b: ByteBuffer, a: ByteArray): Boolean {
            if (b.remaining() != a.size) return true
            return (0 until b.limit()).any { b[it] != a[it] }
        }

        if (somethingChanged(before, after)) {
            cliOutln("UPDATING: $file")
            // Overwrite
            Files.newOutputStream(file).use { it.write(after) }
        } else if (lmt != null) {
            // Update the file timestamp
            Files.setLastModifiedTime(file, FileTime.fromMillis(lmt + 1))
        }
    } else {
		cliOutln("WRITING: $file")
		KratonWriter(Files.newBufferedWriter(file, Charsets.UTF_8)).use { target.generate(it) }
    }
}

/** Returns the string with the first letter uppercase. */
internal val String.upperCaseFirst
    get() = if (this.length <= 1)
        this.toUpperCase()
    else
        "${Character.toUpperCase(this[0])}${this.substring(1)}"