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
import org.gradle.api.tasks.bundling.*
import org.jetbrains.dokka.gradle.*

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath ("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
    }
}

plugins {
    id("org.jetbrains.dokka") version dokkaVersion apply false
}

allprojects {
    group = kraton()
    version = kratonVersion

    evaluationDependsOnChildren()

    repositories {
        mavenCentral()
    }
}

project(":modules").subprojects {
    if (!this@subprojects.name.startsWith("internal")) {
        val isBase = this.name == "base"
        val isGradle = this.name == "gradle"
        val isBaseOrGradle = isBase || isGradle
        val artifactId = if (this@subprojects === project(":modules:base"))
            "kraton"
        else
            "kraton-${this@subprojects.name.replace('.', '-')}"

        apply {
            plugin("java")
            plugin("maven")
            plugin("signing")

            plugin("org.jetbrains.dokka")
        }

        val java = the<JavaPluginConvention>()
        val signing = the<SigningExtension>()

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

            add("archives", artifactNotation(artifactId))

            if (!isGradle) { // The plugin-publish plugin takes care of this for kraton-gradle.
                add("archives", artifactNotation(artifactId, "sources"))
                add("archives", artifactNotation(artifactId, "javadoc"))
            }
        }

        configure<SigningExtension> {
            isRequired = deployment.type == BuildType.RELEASE

            sign(configurations["archives"])
        }

        tasks {
            "jar"(Jar::class) {
                baseName = artifactId
            }

            val sourcesJar = "sourcesJar"(Jar::class) {
                baseName = artifactId
                classifier = "sources"
                from(java.sourceSets["main"].allSource)
            }

            val dokka = "dokka"(DokkaTask::class) {
                outputFormat = "javadoc"
                outputDirectory = "$buildDir/javadoc"
            }

            val javadocJar = "javadocJar"(Jar::class) {
                dependsOn(dokka)

                baseName = artifactId
                classifier = "javadoc"
                from(File(buildDir, "javadoc"))
            }

            "signArchives" {
                dependsOn(sourcesJar, javadocJar)
            }

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
                                if (deployment.type === BuildType.RELEASE) signing.signPom(this)
                            }

                            pom.project {
                                withGroovyBuilder {
                                    "artifactId"(artifactId)

                                    "name"("Kraton")
                                    "description"("A type-safe code generation tool.")
                                    "packaging"("jar")
                                    "url"("https://github.com/TheMrMilchmann/Kraton")

                                    "licenses" {
                                        "license" {
                                            "name"("BSD-3-Clause")
                                            "url"("https://github.com/TheMrMilchmann/Kraton/LICENSE.md")
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

                                    if (!isBaseOrGradle) {
                                        "dependencies" {
                                            "dependency" {
                                                "groupId"(project.group)
                                                "artifactId"("kraton")
                                                "version"(project.version)
                                                "scope"("compile")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

val sourceProjects = listOf(
    project(":modules:base"),
    project(":modules:lang-java")
)

tasks {
    "aggregateDocs"(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = File(rootProject.buildDir, "docs/html").absolutePath

        impliedPlatforms = mutableListOf("JVM")
        jdkVersion = 8

        kotlinTasks(KotlinClosure0({ sourceProjects.map { it.tasks["compileKotlin"] } }))

        sourceProjects.forEach {
            val srcPath = File(it.projectDir, "src/main/kotlin")

            linkMappings.add(LinkMapping().apply {
                dir = srcPath.absolutePath
                url = "https://github.com/TheMrMilchmann/Kraton/blob/master/modules/${it.name}/src/main/kotlin"
                suffix = "#L"
            })
        }
    }
}