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
package com.github.themrmilchmann.kraton.build.config

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.*

fun Project.configureUploadTask(init: (GroovyBuilderScope.() -> Unit)? = null) {
    tasks {
        "uploadArchives"(Upload::class) {
            repositories {
                withConvention(MavenRepositoryHandlerConvention::class) {
                    mavenDeployer {
                        withGroovyBuilder {
                            "repository"("url" to deployment.repo) {
                                "authentication"(
                                    "userName" to deployment.user,
                                    "password" to deployment.password
                                )
                            }
                        }

                        beforeDeployment {
                            if (deployment.type === BuildType.RELEASE) the<SigningExtension>().signPom(this)
                        }

                        pom.project {
                            withGroovyBuilder {
                                "artifactId"("kraton-tools")

                                "name"("Kraton")
                                "description"("A type-safe code generation tool.")
                                "packaging"("jar")
                                "url"("https://github.com/TheMrMilchmann/Kraton")

                                "licenses" {
                                    "license" {
                                        "name"("BSD-3-Clause")
                                        "url"("https://github.com/TheMrMilchmann/Kraton/blob/master/LICENSE.md")
                                        "distribution"("repo")
                                    }
                                }

                                "developers" {
                                    "developer" {
                                        "id"("TheMrMilchmann")
                                        "name"("Leon Linhart")
                                        "email"("themrmilchmann@gmail.com")
                                        "url"("https://github.com/TheMrMilchmann")
                                    }
                                }

                                "scm" {
                                    "connection"("scm:git:git://github.com/TheMrMilchmann/Kraton.git")
                                    "developerConnection"("scm:git:git://github.com/TheMrMilchmann/Kraton.git")
                                    "url"("https://github.com/TheMrMilchmann/Kraton.git")
                                }

                                init?.invoke(this)
                            }
                        }
                    }
                }
            }
        }
    }
}