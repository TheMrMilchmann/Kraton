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
import com.github.themrmilchmann.kraton.build.config.BuildType

plugins {
    `kotlin-delegate`
    `deployment-delegate`
    `java-gradle-plugin`
    `kotlin-dsl`

    id("com.gradle.plugin-publish") version gradlePluginPublishVersion
}

gradlePlugin {
    (plugins) {
        "com.github.themrmilchmann.kraton" {
            id = "com.github.themrmilchmann.kraton"
            implementationClass = "com.github.themrmilchmann.kraton.gradle.KratonGradle"
        }
    }
}

pluginBundle {
    website = "https://github.com/TheMrMilchmann/Kraton"
    vcsUrl = website

    (plugins) {
        "com.github.themrmilchmann.kraton" {
            id = "com.github.themrmilchmann.kraton"
            displayName = "Kraton Gradle Plugin"
            description = "Kraton Gradle Plugin"
            tags = listOf("Kraton")
        }
    }
}

tasks {
    val uploadArchives by tasks.getting
    if (deployment.type == BuildType.RELEASE) uploadArchives.dependsOn("publishPlugins")
}

dependencies {
    compile(project(":tools"))
    allprojects.forEach { if (it.name.startsWith("lang-")) runtime(it) }
}