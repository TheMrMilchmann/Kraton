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
package com.github.themrmilchmann.kraton.gradle.tasks

import com.github.themrmilchmann.kraton.gradle.*
import org.gradle.api.file.*
import org.gradle.api.internal.file.*
import org.gradle.api.internal.file.collections.*
import org.gradle.api.tasks.*
import java.nio.file.*

open class Generate internal constructor(): SourceTask() {

    @get:InputDirectory
    lateinit var outputRoot: Path

    var isWerror: Boolean = false
    var nThreads: Int = 4

    private val fileResolver: FileResolver = IdentityFileResolver()

    @get:Classpath
    private var classpath: FileCollection = DefaultConfigurableFileCollection(fileResolver, null)

    @TaskAction
    fun doRun() {
        val res = project.javaexec {
            main = "com.github.themrmilchmann.kraton.cli.KratonCLI"
            val launchArgs = mutableListOf(
                "generate",
                outputRoot.toString()
            )
            getSource().files.forEach { launchArgs.add(it.absolutePath) }
            if (isWerror) launchArgs.add("--Werror")
            launchArgs.add("--nThreads=$nThreads")

            args = launchArgs
            classpath = project.files(KratonGradle::class.java.protectionDomain.codeSource.location.path) + this@Generate.classpath
        }

        res.assertNormalExitValue()
        res.rethrowFailure()
    }

    fun classpath(vararg paths: Any): Generate {
        classpath += fileResolver.resolveFiles(paths)
        return this
    }

    fun setClasspath(classpath: FileCollection): Generate {
        this.classpath = classpath
        return this
    }

}