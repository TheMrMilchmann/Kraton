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
import codegen.*
import org.gradle.api.internal.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.*
import org.jetbrains.kotlin.gradle.plugin.*

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath ("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
    }
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    maven
    signing

    id("com.gradle.plugin-publish") version gradlePluginPublishVersion
}

apply {
    plugin("org.jetbrains.dokka")
}

group = kraton()
version = kratonVersion

val apiExtensionsOutputDir = file("src/main-generated/kotlin")

(java.sourceSets["main"] as HasConvention).convention
    .getPlugin(KotlinSourceSet::class)
    .kotlin.apply {
    srcDir(apiExtensionsOutputDir)
}

val pluginId = kraton()

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

signing {
    isRequired = deployment.type == BuildType.RELEASE

    sign(configurations["archives"])
}

gradlePlugin {
    (plugins) {
        pluginId {
            id = pluginId
            implementationClass = kraton("gradle.KratonGradlePlugin")
        }
    }
}

pluginBundle {
    website = "https://github.com/TheMrMilchmann/Kraton"
    vcsUrl = website

    (plugins) {
        pluginId {
            id = pluginId
            displayName = "Kraton Gradle Plugin"
            description = "Kraton Gradle Plugin"
            tags = listOf("Kraton")
        }
    }
}

tasks {
    val generateKratonDependencyExtensions = "generateKratonDependencyExtensions"(GeneratePluginDependencyExtensions::class) {
        outputFile = File(apiExtensionsOutputDir, "${kraton("gradle").replace('.', '/')}/KratonDependencyExtensions.kt")
        currentKratonVersion = kratonVersion
    }

    "compileKotlin" {
        dependsOn(generateKratonDependencyExtensions)
    }

    "clean"(Delete::class) {
        delete(apiExtensionsOutputDir)
    }

    val sourcesJar = "sourcesJar"(Jar::class) {
        classifier = "sources"
        from(java.sourceSets["main"].allSource)
    }

    val dokka = "dokka"(DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"
    }

    val javadocJar = "javadocJar"(Jar::class) {
        dependsOn(dokka)

        classifier = "javadoc"
        from(File(buildDir, "javadoc"))
    }

    "signArchives" {
        dependsOn(sourcesJar, javadocJar)
    }

    "uploadArchives"(Upload::class) {
        if (deployment.type == BuildType.RELEASE) dependsOn("publishPlugins")

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
                            "artifactId"(project.name)

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
                        }
                    }
                }
            }
        }
    }

}