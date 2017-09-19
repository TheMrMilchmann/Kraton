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
import org.gradle.api.internal.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import java.io.*
import java.util.*
import org.jetbrains.kotlin.gradle.plugin.*

plugins {
    `kotlin-dsl`
}

(the<JavaPluginConvention>().sourceSets["main"] as HasConvention).convention.getPlugin(KotlinSourceSet::class.java).kotlin.apply {
    srcDir(File("src/main-generated/kotlin"))
}

fun String.toComment(indent: String = "") =
    if (lines().size == 1)
        "$indent/* $this */"
    else
        "$indent/*\n${StringBuilder().apply {
            this@toComment.lines().forEach { appendln("$indent * $it") }
        }}$indent */"

tasks {
    val generatePropertyConstants = "generatePropertyConstants" {
        doLast {
            val properties = Properties().apply {
                FileInputStream(File(projectDir.parentFile, "gradle.properties")).use {
                    load(it)
                }
            }

            File(projectDir, "src/main-generated/kotlin/KratonConstantProperties.kt").apply {
                parentFile.mkdirs()
                createNewFile()

                writeText(
                    """${File(projectDir.parentFile, ".ci/resources/LICENSE_HEADER_GEN.txt").readText().toComment()}

${
                    StringJoiner("\n").apply {
                        properties.forEach { k, v -> add("const val $k = \"$v\"") }
                    }
                    }""")
            }
        }
    }

    val copy = "copyImplicitAdditions"(Copy::class) {
        dependsOn(generatePropertyConstants)

        from(File(projectDir, "src/main/kotlin"))
        from(File(projectDir, "src/main-generated/kotlin"))

        into(File(projectDir.parentFile, "modules/gradle/buildSrc/src/main-implicit/kotlin"))

        exclude("build/**")

        outputs.upToDateWhen { false }
    }

    "compileKotlin" {
        dependsOn(copy)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.4-3")
}