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
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.testing.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.*
import testNGVersion

class KratonLangModule : Plugin<Project> {

    override fun apply(target: Project) {
        target.run {
            pluginManager.apply(KotlinDelegate::class.java)
            pluginManager.apply(DeploymentDelegate::class.java)

            configureUploadTask {
                "dependencies" {
                    "dependency" {
                        "groupId"(project.group)
                        "artifactId"("kraton")
                        "version"(project.version)
                        "scope"("compile")
                    }
                }
            }

            configure<KotlinProjectExtension> {
                experimental.coroutines = Coroutines.ENABLE
            }

            val java = the<JavaPluginConvention>()
            val srcSetMain = java.sourceSets["main"]
            val srcSetUnitTests = java.sourceSets["test"]

            val testIntegrationSources = java.sourceSets.create("test-integration") {
                compileClasspath += srcSetUnitTests.compileClasspath
                runtimeClasspath += srcSetMain.runtimeClasspath + srcSetUnitTests.runtimeClasspath
            }

            tasks {
                val test = "test"(Test::class) {
                    useTestNG()
                }

                /*
                val testIntegration = "test-integration"(Test::class) {
                    useTestNG()

                    testClassesDirs = project(":internal-test").the<JavaPluginConvention>().sourceSets["main"].output
                    classpath = testIntegrationSources.runtimeClasspath

                    systemProperty("rootDir", rootProject.projectDir.absolutePath)
                    systemProperty("module", project.name)
                }

                "check" {
                    dependsOn(test, testIntegration)
                }*/
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                "compile"(project(":base"))
                "testCompile"("org.testng", "testng", testNGVersion)
            }
        }
    }

}