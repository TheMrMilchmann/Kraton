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
package com.github.themrmilchmann.kraton.lang.java

import com.github.themrmilchmann.kraton.lang.java.impl.model.*
import com.github.themrmilchmann.kraton.lang.jvm.*

val public = Modifiers.PUBLIC.jlm
val protected = Modifiers.PROTECTED.jlm
val private = Modifiers.PRIVATE.jlm
val static = Modifiers.STATIC.jlm
val abstract = Modifiers.ABSTRACT.jlm
val default = Modifiers.DEFAULT.jlm
val synchronized = Modifiers.SYNCHRONIZED.jlm
val transient = Modifiers.TRANSIENT.jlm
val volatile = Modifiers.VOLATILE.jlm
val final = Modifiers.FINAL.jlm
val native = Modifiers.NATIVE.jlm
val strictfp = Modifiers.STRICTFP.jlm

// Modifiers for modules
val open = Modifiers.OPEN.jlm
val transitive = Modifiers.TRANSITIVE.jlm

val Override = Annotate(java.lang.Override::class.asType)

open class Annotate(internal val type: IJvmType, internal val params: String? = null): JavaModifier

private val Modifiers.jlm get() = JavaLanguageModifier(this)
class JavaLanguageModifier internal constructor(internal val mod: Modifiers) : JavaModifier

interface JavaModifier

abstract class JavaModifierTarget {
    internal abstract fun setModifiers(vararg mods: JavaModifier)
}

operator fun <T : JavaModifier, R : JavaModifierTarget> T.rangeTo(trg: R) = trg.apply { setModifiers(this@rangeTo) }
operator fun <T : JavaModifier, R : JavaModifierTarget> Array<T>.rangeTo(trg: R) = trg.apply { setModifiers(*this@rangeTo) }

inline operator fun <reified T : JavaModifier> T.rangeTo(other: T) = arrayOf(this, other)
inline operator fun <reified T : JavaModifier> Array<T>.rangeTo(other: T) = arrayOf(*this, other)