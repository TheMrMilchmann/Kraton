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
package com.github.themrmilchmann.kraton.build.plugins

import com.github.themrmilchmann.kraton.build.config.*
import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.jvm.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.*
import org.jetbrains.dokka.gradle.*
import java.io.*

class DeploymentDelegate : Plugin<Project> {

    private val Project.java get() = the<JavaPluginConvention>()

    override fun apply(target: Project) {
        target.run {
            pluginManager.apply(MavenPlugin::class.java)
            pluginManager.apply(SigningPlugin::class.java)

            artifacts {
                fun artifactNotation(artifact: String, classifier: String? = null) =
                    if (classifier == null) {
                        mapOf(
                            "file" to File(buildDir, "libs/$artifact-$version.jar"),
                            "name" to artifact,
                            "type" to "jar"
                        )
                    } else {
                        mapOf(
                            "file" to File(buildDir, "libs/$artifact-$version-$classifier.jar"),
                            "name" to artifact,
                            "type" to "jar",
                            "classifier" to classifier
                        )
                    }

                add("archives", artifactNotation(project.name))
                add("archives", artifactNotation(project.name, "sources"))
                add("archives", artifactNotation(project.name, "javadoc"))
            }

            configure<SigningExtension> {
                isRequired = deployment.type == BuildType.RELEASE
                sign(configurations["archives"])
            }

            setupTasks()
        }
    }

    private fun Project.setupTasks() {
        tasks {
            "jar"(Jar::class) {
                baseName = project.name
            }

            val sourcesJar = "sourcesJar"(Jar::class) {
                baseName = project.name
                classifier = "sources"
                from(java.sourceSets["main"].allSource)
            }

            val dokka = "dokka"(DokkaTask::class) {
                outputFormat = "javadoc"
                outputDirectory = "$buildDir/javadoc"
            }

            "javadoc" {
                dependsOn(dokka)
            }

            val javadocJar = "javadocJar"(Jar::class) {
                dependsOn(dokka)

                baseName = project.name
                classifier = "javadoc"
                from(File(buildDir, "javadoc"))
            }

            "signArchives" {
                dependsOn(sourcesJar, javadocJar)
            }
        }
    }

}