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
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*

plugins {
	`kotlin-dsl`
    `java-gradle-plugin`

    id("com.gradle.plugin-publish") version gradlePluginPublishVersion
}

val apiExtensionsOutputDir = file("src/main-generated/kotlin")

(java.sourceSets["main"] as HasConvention).convention
    .getPlugin(KotlinSourceSet::class)
    .kotlin.apply {
    srcDir(apiExtensionsOutputDir)
}

val pluginId = kraton()

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
    val generateKratonDependencyExtensions = "generateKratonDependencyExtensions"(GenerateKratonDependencyExtensions::class) {
        outputFile = File(apiExtensionsOutputDir, "${kraton("gradle").replace('.', '/')}/KratonDependencyExtensions.kt")
        embeddedKratonVersion = kratonVersion
    }

    "compileKotlin" {
        dependsOn(generateKratonDependencyExtensions)
    }

    "clean"(Delete::class) {
        delete(apiExtensionsOutputDir)
    }
}