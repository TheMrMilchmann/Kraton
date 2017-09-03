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
package com.github.themrmilchmann.kraton

import joptsimple.*
import java.io.*
import java.lang.reflect.*
import java.nio.file.*
import kotlin.streams.*

internal val TEMPLATE_PATH_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**/*.{kt,java}")

data class GeneratorConfiguration(
    val isForce: Boolean,
    val isWerror: Boolean,
    val logMode: LogMode,
    val outputDir: Path,
    val profiles: List<() -> Profile>,
    val generatorSource: Path?
) {

    companion object {

        @JvmStatic
        internal fun resolve(args: Array<String>) =
            OptionParser().run {
                val pathConverter = object: ValueConverter<Path> {

                    override fun convert(value: String?) = if (value != null) Paths.get(value) else null
                    override fun valuePattern() = null
                    override fun valueType() = Path::class.java

                }

                val cliGeneratorSource = acceptsAll(listOf("g", "generator-source"))
                    .withRequiredArg()
                    .withValuesConvertedBy(pathConverter)
                val cliIsForce = acceptsAll(listOf("f", "force"))
                    .withOptionalArg()
                    .ofType(Boolean::class.java)
                    .defaultsTo(true)
                val cliIsWerror = acceptsAll(listOf("g", "Werror"))
                    .withOptionalArg()
                    .ofType(Boolean::class.java)
                    .defaultsTo(true)
                val cliLogMode = accepts("level")
                    .withRequiredArg()
                    .ofType(LogMode::class.java)
                    .withValuesConvertedBy(object: ValueConverter<LogMode> {

                    override fun convert(value: String?) = if (value != null) LogMode.fromString(value) else null
                    override fun valuePattern() = null
                    override fun valueType() = LogMode::class.java

                })
                val cliOutputDir = acceptsAll(listOf("o", "output-dir"))
                    .withRequiredArg()
                    .withValuesConvertedBy(pathConverter)
                    .required()

                val tmpCliTemplateClasses = accepts("template-classes")
                val cliTemplatePath = acceptsAll(listOf("t", "template-path"))
                    .requiredUnless(tmpCliTemplateClasses)
                    .availableUnless(tmpCliTemplateClasses)
                    .withRequiredArg()
                    .withValuesConvertedBy(pathConverter)
                val cliTemplateClasses = tmpCliTemplateClasses.requiredUnless(cliTemplatePath)
                    .availableUnless(cliTemplatePath)
                    .withRequiredArg()
                    .ofType(String::class.java)
                    .withValuesSeparatedBy(';')

                val cliHelp = acceptsAll(listOf("help", "h", "?"))
                    .forHelp()

                val optionSet = parse(*args)

                /* Do not run the generator when the "help" option is present! */
                if (optionSet.has(cliHelp)) System.exit(0)

                val outputDir = optionSet.valueOf(cliOutputDir)

                val generatorSource =
                    if (optionSet.has(cliGeneratorSource))
                        optionSet.valueOf(cliGeneratorSource)
                    else
                        null
                val isForce = if (optionSet.has(cliIsForce)) optionSet.valueOf(cliIsForce) else false
                val isWerror = if (optionSet.has(cliIsWerror)) optionSet.valueOf(cliIsWerror) else false
                val logMode = if (optionSet.has(cliLogMode)) optionSet.valueOf(cliLogMode) else LogMode.INFO

                fun cliLogWarn(msg: String, t: Throwable) {
                    cliOutln(msg)

                    if (logMode >= LogMode.DEBUG) t.printStackTrace()
                    if (isWerror) System.exit(-1)
                }

                fun methodFilter(method: Method, javaClass: Class<*>) =
                    // static
                    method.modifiers and Modifier.STATIC != 0 &&
                        // returns Profile
                        method.returnType === javaClass &&
                        // has no arguments
                        method.parameterTypes.isEmpty()

                val NO_METHODS = emptyList<Method>()

                val profiles =
                    (if (optionSet.has(cliTemplateClasses))
                        optionSet.valuesOf(cliTemplateClasses)
                            .stream()
                            .map { null to it }
                    else
                        optionSet.valuesOf(cliTemplatePath).stream()
                            .flatMap { templateRoot ->
                                Files.walk(templateRoot)
                                    .filter {
                                        Files.isReadable(it) && Files.isRegularFile(it)
                                            && TEMPLATE_PATH_MATCHER.matches(it)
                                    }.map {
                                        it to templateRoot.relativize(it)
                                            .toString()
                                            .replace(File.separatorChar, '.')
                                            .removePrefix(".") // Probably not needed on most environments (if at all).
                                            .substringBeforeLast('.') // Removes the file extensions (.java, .kt, etc.)
                                }
                            }
                    ).flatMap {
                        val source = it.first
                        val name = it.second

                        try {
                            Class.forName(name).methods.asSequence()
                                .filter { methodFilter(it, Profile::class.java) }
                                .asStream()
                                .map { source to it }
                        } catch (e: ClassNotFoundException) {
                            /*
                             * If --template-path was used, we're trying this again since kotlin files may not map to
                             * classes but --template-classes should be used with the actual class names.
                             */
                            if (optionSet.has(cliTemplatePath)) {
                                try {
                                    Class.forName(name + "Kt").methods.asSequence()
                                        .filter { methodFilter(it, Profile::class.java) }
                                        .asStream()
                                        .map { source to it }
                                } catch (e: ClassNotFoundException) {
                                    if (logMode >= LogMode.INFO)
                                        cliLogWarn("Class with name \"${name + "Kt"}\" not found.", e)

                                    NO_METHODS.stream()
                                        .map { source to it }
                                }
                            } else {
                                if (logMode >= LogMode.INFO)
                                    cliLogWarn("Class with name \"$name\" not found.", e)

                                NO_METHODS.stream()
                                    .map { source to it }
                            }
                        }
                    }.map { (sourcePath, method) -> { (method.invoke(null) as Profile).apply {
                        source = sourcePath
                    }}}
                        .toList()

                GeneratorConfiguration(isForce, isWerror, logMode, outputDir, profiles, generatorSource)
            }

    }

}

enum class LogMode(
    private val string: String
) {
    QUIET("quiet"),
    INFO("info"),
    DEBUG("debug");

    companion object {
        fun fromString(value: String) = when (value) {
            "quiet" -> QUIET
            "info" -> INFO
            "debug" -> DEBUG
            else -> null
        }
    }

}