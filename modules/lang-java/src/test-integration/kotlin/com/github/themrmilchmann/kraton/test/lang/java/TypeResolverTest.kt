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
package com.github.themrmilchmann.kraton.test.lang.java

import com.github.themrmilchmann.kraton.lang.*
import com.github.themrmilchmann.kraton.lang.java.*
import com.github.themrmilchmann.kraton.lang.jvm.*

private const val packageName = "com.github.themrmilchmann.kraton.test.lang.java.types"
private const val srcFolder = "lang-java/build"
private const val srcSet = "kraton/generated"

val aMap = com.github.themrmilchmann.kraton.test.lang.java.types.a.Map::class.asType
val aMapEntry = com.github.themrmilchmann.kraton.test.lang.java.types.a.Map.Entry::class.asType

val bMap = com.github.themrmilchmann.kraton.test.lang.java.types.b.Map::class.asType
val bMapEntry = com.github.themrmilchmann.kraton.test.lang.java.types.b.Map.Entry::class.asType

val TypeResolver = TemplateFile {

    javaClass(
        "TAMBM",
        packageName,
        "$srcFolder/$srcSet"
    ) {

        aMap { "aMap"() }
        bMap { "bMap"() }

    }

    javaClass(
        "TAMBE",
        packageName,
        "$srcFolder/$srcSet"
    ) {

        aMap { "aMap"() }
        bMapEntry { "bMapEntry"() }

    }

}