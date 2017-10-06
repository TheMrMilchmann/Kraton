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
import java.nio.*
import java.nio.file.*
import java.nio.file.attribute.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * The command line entry point of the generator.
 *
 * @since 1.0.0
 */
fun main(args: Array<String>) {
    cliOutln(CLI_HEADER)

    val cfg = GeneratorConfiguration.resolve(args)

    generate(cfg) {
        val pool = ForkJoinPool.commonPool()

        try {
            val errors = AtomicInteger()

            CountDownLatch(cfg.profiles.size).let { latch ->
                fun generate(factory: () -> Profile) {
                    pool.submit {
                        try {
                            generate(factory.invoke())
                        } catch (t: Throwable) {
                            errors.incrementAndGet()
                            t.printStackTrace()
                        }

                        latch.countDown()
                    }
                }

                cfg.profiles.forEach(::generate)
                latch.await()
            }

            if (errors.get() != 0) throw RuntimeException("Generation failed")
        } finally {
            pool.shutdown()
        }
    }
}

private fun generate(
    cfg: GeneratorConfiguration,
    generate: Generator.() -> Unit
) {
    Generator(cfg).generate()
}

private class Generator(
    val cfg: GeneratorConfiguration
) {

    private val GENERATOR_LAST_MODIFIED = cfg.generatorSource?.lastModified ?: 0L

    internal fun generate(profile: Profile) {
        val profileLMT = profile.source?.lastModified ?: 0L

        profile.targets.forEach {
            val outputFile = cfg.outputDir.resolve(StringJoiner("/").run {
                add(it.srcFolder)
                if (it.srcSet.isNotEmpty()) add(it.srcSet)
                add(it.language)
                add(it.packageName.replace('.', File.separatorChar))
                add("${it.fileName}.${it.appendix}")

                toString()
            })

            val lmt = maxOf(profileLMT, GENERATOR_LAST_MODIFIED)
            val outputLMT = if (Files.isRegularFile(outputFile)) outputFile.lastModified else 0L

            if (cfg.isForce || lmt > outputLMT) {
                val nextLMT = maxOf(lmt, outputLMT)

                generateOutput(it, outputFile, nextLMT) { it.printTarget() }
            }
        }
    }

}

private val Path.lastModified
    get() = if (Files.isRegularFile(this))
        Files.getLastModifiedTime(this).toMillis()
    else
        0L

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

private class KratonWriter(out: Writer) : PrintWriter(out) {

    /**
     * Terminates the current line by writing the line separator string. The
     * line separator string is always a single newline character
     * (<code>'\n'</code>).
     */
    override fun println() = print("\n")

}

private fun <T> generateOutput(target: T, file: Path, lmt: Long, generate: T.(PrintWriter) -> Unit) {
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
        }

        // Update the file timestamp
        Files.setLastModifiedTime(file, FileTime.fromMillis(lmt))
    } else {
        cliOutln("WRITING: $file")
        KratonWriter(Files.newBufferedWriter(file, Charsets.UTF_8)).use { target.generate(it) }
    }
}