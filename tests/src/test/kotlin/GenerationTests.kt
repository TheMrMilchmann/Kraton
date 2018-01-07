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
@file:Suppress("UNCHECKED_CAST")

import com.github.themrmilchmann.kraton.io.*
import com.github.themrmilchmann.kraton.lang.*
import com.github.themrmilchmann.kraton.tools.*
import org.testng.annotations.*
import org.testng.Assert.*
import org.testng.*
import java.io.*
import java.lang.reflect.*
import java.nio.file.*
import java.util.stream.*

@Factory
@Parameters("rootDir")
fun testGeneration(rootDir: String): Array<Any> {
    val dRoot = Paths.get(rootDir)
    val dModules = dRoot.resolve("modules")
    val generator = KGenerator(System.out.logger)

    fun holdsIntegrationTests(p: Path): Boolean = Files.isDirectory(p.resolve("src/test-integration"))
    fun gatherIntegrationTestSourceFiles(p: Path): Stream<Pair<Path, Path>> =
        p.resolve("src/test-integration/kotlin/").run {
            Files.walk(this)
                .filter { Files.isRegularFile(it) }
                .map { this.relativize(it) to dRoot.relativize(p) }
        }

    fun filterTemplateMethods(m: Method): Boolean =
        Modifier.isStatic(m.modifiers) && m.returnType === TemplateFile::class.java && m.parameterCount == 0

    return Files.list(dModules)
        .filter(::holdsIntegrationTests)
        .flatMap(::gatherIntegrationTestSourceFiles)
        .flatMap {
            val dTemplateFile = it.first
            val dModule = it.second

            (try {
                Class.forName(dTemplateFile.toString().replace(File.separatorChar, '.').removeSuffix(".kt") + "Kt")
                    .methods.filter(::filterTemplateMethods)
                    .map { { it.invoke(null) } as () -> TemplateFile }
                    .map { it.invoke().let { GenerationTest(generator, dRoot, dTemplateFile, dModule, it, it.templates.map { it.outputSourceSet to it.outputFile }) } }
            } catch (t: Throwable) {
                listOf(EarlyFailure(dTemplateFile, dModule, t))
            }).stream()
        }.toArray({ length -> arrayOfNulls<ITest>(length) })
}

class GenerationTest internal constructor(
    private val generator: KGenerator,
    private val dRoot: Path,
    private val pTemplateFile: Path,
    private val dModule: Path,
    private val template: TemplateFile,
    private val outputs: List<Pair<String, String>>
) : ITest {

    override fun getTestName() = dModule.resolve(pTemplateFile).toString()

    @Test
    fun doRun() {
        val err = generator.exec(KGenerator.Configuration(
            outputRoot = dRoot.resolve("modules"),
            isWerror = true,
            nThreads = 1,
            templates = listOf({ template })
        ))

        assertFalse(err < 0, "Generation failed with negative error code: $err")

        fun String.toFilepath(): String =
            this.substringBeforeLast('.').replace('.', '/') + "." + this.substringAfterLast('.')

        outputs.forEach {
            val pExpected = dModule.resolve("src/test-integration/resources/").resolve(it.second.toFilepath())
            val pActual = dRoot.resolve("modules").resolve("${it.first}/${it.second}".toFilepath())

            val rExpected = Files.newBufferedReader(dRoot.resolve(pExpected))
            val rActual = Files.newBufferedReader(pActual)

            var line = 0
            var lExpected: String? = null
            var lActual: String? = null

            do {
                line++
                lExpected = rExpected.readLine() ?: break
                lActual = rActual.readLine() ?: break

                for (i in 0..maxOf(lExpected.length, lActual.length)) {
                    if (i >= lExpected.length && i >= lActual.length) break

                    if ((i < lExpected.length && i >= lActual.length) ||
                        (i >= lExpected.length && i < lActual.length) ||
                        (lExpected[i] != lActual[i]))
                        throw Error("Mismatch starting at line $line character $i ($it)")
                }
            } while (lExpected !== null && lActual !== null)

            if (lExpected === null && lExpected !== lActual)
                throw Error("Mismatch starting at line $line ($it)")
        }
    }

}

class EarlyFailure internal constructor(
    private val pTemplateFile: Path,
    private val dModule: Path,
    private val cause: Throwable
) : ITest {
    override fun getTestName() = dModule.resolve(pTemplateFile).toString()
    @Test fun doRun() { throw cause }
}