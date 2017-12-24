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
import com.github.themrmilchmann.kraton.gradle.tasks.utils.*
import com.github.themrmilchmann.kraton.tools.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import java.nio.file.*

open class Generate internal constructor(): DefaultTask() {

    @Input
    var source: TaskSource = NO_SOURCE

    @InputDirectory
    lateinit var outputRoot: Path

    var isWerror: Boolean = false
    var nThreads: Int = 4

    @TaskAction
    fun doRun() {
        val generator = KGenerator(project.loggerDelegate)

        val cfg = KGenerator.Configuration(
            outputRoot = outputRoot,
            isWerror = isWerror,
            nThreads = nThreads,
            templates = source.templates
        )

        if (generator.exec(cfg) < 0) throw TaskExecutionException(this, Exception("Unexpected error code"))
    }

}