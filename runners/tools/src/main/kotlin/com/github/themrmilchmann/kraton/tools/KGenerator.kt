/*
 * Copyright (c) 2017 Leon Linhart,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.themrmilchmann.kraton.tools

import com.github.themrmilchmann.kopt.*
import com.github.themrmilchmann.kraton.io.*
import com.github.themrmilchmann.kraton.lang.*
import java.io.*
import java.lang.reflect.*
import java.nio.*
import java.nio.file.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import java.util.stream.*

class KGenerator(override val logger: ILoggingService) : AbstractTool<KGenerator.Configuration>() {

    private fun methodFilter(method: Method) =
        method.modifiers and Modifier.STATIC != 0 &&
            method.returnType === TemplateFile::class.java &&
            method.parameterTypes.isEmpty()

    override val optionPool: Pair<OptionPool, (OptionSet) -> Configuration> by lazy {
        val outputRoot = Argument.Builder { Paths.get(it) }.create()
        val sourceClasses = Argument.Builder(Parser.STRING).create()

        val isWerror = Option.Builder("Werror", Parser.BOOLEAN).withDefaultValue(false).withMarkerValue(true, true).create()
        val nThreads = Option.Builder("nThreads", Parser.INT).withDefaultValue(4).create()

        OptionPool.Builder()
            .withArg(outputRoot)
            .withVararg(sourceClasses)
            .withOption(isWerror)
            .withOption(nThreads)
            .create() to { set: OptionSet ->
            Configuration(
                outputRoot = set[outputRoot]!!,
                isWerror = set.getOrDefault(isWerror)!!,
                nThreads = set.getOrDefault(nThreads)!!,
                templates = set.getVarargValues(sourceClasses).map {
                    try {
                        Class.forName(it)
                    } catch (t: Throwable) {
                        logger.warn("Class \"$it\" not found.")
                        null
                    }
                }.flatMap {
                    it?.run {
                        methods.filter(::methodFilter)
                            .map {{ (it.invoke(null) as TemplateFile) }}
                    } ?: emptyList()
                }
            )
        }
    }

    override fun exec(config: Configuration): Int {
        val pool = Executors.newFixedThreadPool(config.nThreads)
        val templateFiles: Stream<TemplateFile> = config.templates.stream().map { it.invoke() }

        try {
            val errors = AtomicInteger()

            return CountDownLatch(config.templates.size).let { latch ->
                templateFiles.forEach {
                    pool.submit {
                        try {
                            it.generate(config)
                        } catch (t: Throwable) {
                            logger.error("Error while processing template", t)
                            errors.getAndIncrement()
                        }

                        latch.countDown()
                    }
                }

                latch.await()
                if (errors.get() > 0) -1 else 0
            }
        } finally {
            pool.shutdown()
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

    private fun TemplateFile.generate(config: Configuration) {
        fun String.toFilepath(): String =
            this.substringBeforeLast('.').replace('.', '/') + "." + this.substringAfterLast('.')

        templates.forEach {
            val outputFile = Paths.get("${config.outputRoot}/${it.outputSourceSet}/${it.outputFile}".toFilepath())
            outputFile.parent.let {
                if (!Files.isDirectory(it)) {
                    Files.createDirectories(it)
                    logger.debug("MKDIRS: $it")
                }
            }

            if (Files.isRegularFile(outputFile)) {
                val inMemoryBuffer = ByteArrayOutputStream(4 * 1024)
                it.print(BufferedWriter(OutputStreamWriter(inMemoryBuffer, Charsets.UTF_8)))

                val before = readFile(outputFile)
                val after = inMemoryBuffer.toByteArray()

                fun somethingChanged(b: ByteBuffer, a: ByteArray): Boolean {
                    if (b.remaining() != a.size) return true
                    return (0 until b.limit()).any { b[it] != a[it] }
                }

                if (somethingChanged(before, after)) {
                    logger.debug("UPDATING: $outputFile")
                    Files.newOutputStream(outputFile).use { it.write(after) }
                }
            } else {
                logger.debug("WRITING: $outputFile")
                it.print(Files.newBufferedWriter(outputFile, Charsets.UTF_8))
            }
        }
    }

    data class Configuration(
        val outputRoot: Path,
        val isWerror: Boolean,
        val nThreads: Int,
        val templates: List<() -> TemplateFile>
    )

}