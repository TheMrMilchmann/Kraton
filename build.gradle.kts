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
import com.github.themrmilchmann.kraton.build.config.*
import com.github.themrmilchmann.kraton.build.plugins.*
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

allprojects {
    group = "com.github.themrmilchmann.kraton"
    version = kratonVersion

    evaluationDependsOnChildren()

    repositories {
        maven("https://dl.bintray.com/cbeust/maven/") // TestNG
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

tasks {
    "buildDocumentation"(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = File(rootProject.buildDir, "docs/html").absolutePath

        impliedPlatforms = mutableListOf("JVM")
        jdkVersion = 9

        subprojects.filter { it == project(":base") || it.plugins.hasPlugin(KratonLangModule::class.java) }.forEach { project ->
            val srcPath = File(project.projectDir, "src/main/kotlin")

            linkMappings.add(LinkMapping().apply {
                dir = srcPath.absolutePath
                url = "https://github.com/TheMrMilchmann/Kraton/blob/master/modules/${project.name}/src/main/kotlin"
                suffix = "#L"
            })
        }
    }
}