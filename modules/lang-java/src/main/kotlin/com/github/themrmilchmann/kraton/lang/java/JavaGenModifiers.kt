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
package com.github.themrmilchmann.kraton.lang.java

import com.github.themrmilchmann.kraton.lang.jvm.*
import java.io.*
import java.util.*
import kotlin.reflect.*

/**
 * The `public` modifier.
 *
 * @since 1.0.0
 */
val public = JavaVisibilityModifier("public")

/**
 * The `public` modifier.
 *
 * @since 1.0.0
 */
@JvmField val PUBLIC = public

/**
 * The `protected` modifier.
 *
 * @since 1.0.0
 */
val protected = JavaVisibilityModifier("protected")

/**
 * The `protected` modifier.
 *
 * @since 1.0.0
 */
@JvmField val PROTECTED = protected

/**
 * The `private` modifier.
 *
 * @since 1.0.0
 */
val private = JavaVisibilityModifier("private")

/**
 * The `private` modifier.
 *
 * @since 1.0.0
 */
@JvmField val PRIVATE = private

/**
 * The `static` modifier.
 *
 * @since 1.0.0
 */
val static = JavaVisibilityModifier("static")

/**
 * The `static` modifier.
 *
 * @since 1.0.0
 */
@JvmField val STATIC = static

/**
 * The `abstract` modifier.
 *
 * @since 1.0.0
 */
val abstract = JavaVisibilityModifier("abstract")

/**
 * The `abstract` modifier.
 *
 * @since 1.0.0
 */
@JvmField val ABSTRACT = abstract

/**
 * The `final` modifier.
 *
 * @since 1.0.0
 */
val final = JavaVisibilityModifier("final")

/**
 * The `final` modifier.
 *
 * @since 1.0.0
 */
@JvmField val FINAL = final

/**
 * The `transient` modifier.
 *
 * @since 1.0.0
 */
val transient = JavaVisibilityModifier("transient")

/**
 * The `transient` modifier.
 *
 * @since 1.0.0
 */
@JvmField val TRANSIENT = transient

/**
 * The `volatile` modifier.
 *
 * @since 1.0.0
 */
val volatile = JavaVisibilityModifier("volatile")

/**
 * The `volatile` modifier.
 *
 * @since 1.0.0
 */
@JvmField val VOLATILE = volatile

/**
 * The `default` modifier.
 *
 * @since 1.0.0
 */
val default = JavaVisibilityModifier("default")

/**
 * The `default` modifier.
 *
 * @since 1.0.0
 */
@JvmField val DEFAULT = default

/**
 * The `synchronized` modifier.
 *
 * @since 1.0.0
 */
val synchronized = JavaVisibilityModifier("synchronized")

/**
 * The `synchronized` modifier.
 *
 * @since 1.0.0
 */
@JvmField val SYNCHRONIZED = synchronized

/**
 * The `native` modifier.
 *
 * @since 1.0.0
 */
val native = JavaVisibilityModifier("native")

/**
 * The `native` modifier.
 *
 * @since 1.0.0
 */
@JvmField val NATIVE = native

/**
 * The `strictfp` modifier.
 *
 * @since 1.0.0
 */
val strictfp = JavaVisibilityModifier("strictfp")

/**
 * The `strictfp` modifier.
 *
 * @since 1.0.0
 */
@JvmField val STRICTFP = strictfp

/**
 * The `@Deprecated` annotation.
 *
 * @since 1.0.0
 */
class Deprecated(
    private val since: String? = null,
    private val forRemoval: Boolean? = null
) : Annotate(
    JvmTypeReference("Deprecated", "java.lang"),
    if (since != null || forRemoval != null)
        StringJoiner(", ").run {
            if (since != null) add("since=$since")
            if (forRemoval != null) add("forRemoval=$forRemoval")

            toString()
        }
    else
        null
)

/**
 * The `@Override` annotation.
 *
 * @since 1.0.0
 */
val Override = Annotate(Override::class.asType)

/**
 * This modifier may be used to add annotations to an object.
 *
 * @property type the type of the annotations
 * @property parameters the inner content of the annotation
 *
 * @since 1.0.0
 */
open class Annotate @JvmOverloads constructor(
    private val type: IJvmType,
    private val parameters: String? = null
) : JavaModifier({ import(type) }) {

    override fun applyTo(target: JavaModifierTarget) {
        target.annotations.add(this)
    }

    /**
     * Returns a pre-formatted annotation string.
     *
     * @return the annotation pre-formatted to be generator friendly
     *
     * @since 1.0.0
     */
    fun asString(containerType: JavaTopLevelType<*, *>?) =
        "@${type.asString(containerType)}${if (parameters === null) "" else "($parameters)"}"

}

/**
 * A modifier of this type may be applied to mutate the visibility of a java object.
 *
 * @property name the name of this modifier
 *
 * @since 1.0.0
 */
class JavaVisibilityModifier internal constructor(
    internal val name: String
) : JavaModifier() {

    /**
     * Returns the name of this modifier.
     *
     * @since 1.0.0
     */
    override fun toString() = name

}

/**
 * A modifier that may be applied to a java object.
 *
 * Modifiers can represent either modifiers, annotations or extra.
 *
 * @since 1.0.0
 */
abstract class JavaModifier internal constructor(
    internal val applyImports: JavaTopLevelType<*, *>.() -> Unit = {}
) {

    /**
     * Applies this modifier to the given target.
     *
     * @param target the target to apply this modifier to.
     *
     * @since 1.0.0
     */
    internal open fun applyTo(target: JavaModifierTarget) {
        target.modifiers.put(this::class, this)
    }

}

/**
 * A JavaModifierTarget represents java object to which modifiers may be applied to.
 *
 * @since 1.0.0
 */
abstract class JavaModifierTarget {

    internal val annotations = mutableListOf<Annotate>()
    internal val modifiers = mutableMapOf<KClass<out JavaModifier>, JavaModifier>()
    internal fun setModifiers(vararg modifiers: JavaModifier) {
        modifiers.forEach { it.applyTo(this) }
    }

    internal inline infix fun <reified M : JavaModifier> has(modifier: M) = modifiers[M::class] === modifier

    internal inline fun <reified M : JavaModifier> has() = modifiers.containsKey(M::class)
    internal inline fun <reified M : JavaModifier> get() = modifiers[M::class] as M

    internal fun PrintWriter.printAnnotations(indent: String, containerType: JavaTopLevelType<*, *>) {
        if (annotations.isNotEmpty()) {
            print(indent)
            print(StringJoiner(LN + indent).run {
                annotations.forEach { add(it.asString(containerType)) }
                toString()
            })
            print(LN)
        }
    }

    internal fun printAnnotationsInline(containerType: JavaTopLevelType<*, *>) =
        if (annotations.isNotEmpty()) {
            val annotations = StringJoiner(" ").run {
                annotations.forEach { add(it.asString(containerType)) }
                toString()
            }

            annotations + " "
        } else
            ""

    internal fun PrintWriter.printModifiers() {
        if (modifiers.values.any { it is JavaVisibilityModifier }) {
            val modifiers = StringJoiner(" ").run {
                if (has(public)) add(public.toString())
                if (has(protected)) add(protected.toString())
                if (has(private)) add(private.toString())
                if (has(static)) add(static.toString())
                if (has(abstract)) add(abstract.toString())
                if (has(final)) add(final.toString())
                if (has(transient)) add(transient.toString())
                if (has(volatile)) add(volatile.toString())
                if (has(default)) add(default.toString())
                if (has(synchronized)) add(synchronized.toString())
                if (has(native)) add(native.toString())
                if (has(strictfp)) add(strictfp.toString())

                toString()
            }

            print(modifiers)

            if (modifiers.isNotEmpty()) print(" ")
        }
    }

}

/**
 * Utility method to chain modifiers using the rangeTo operator.
 *
 * @since 1.0.0
 */
operator fun <T : JavaModifier, R : JavaModifierTarget> T.rangeTo(trg: R) = trg.apply { setModifiers(this@rangeTo) }

/**
 * Utility method to chain modifiers using the rangeTo operator.
 *
 * @since 1.0.0
 */
operator fun <T : JavaModifier, R : JavaModifierTarget> Array<T>.rangeTo(trg: R) = trg.apply { setModifiers(*this@rangeTo) }

/**
 * Utility method to chain modifiers using the rangeTo operator.
 *
 * @since 1.0.0
 */
inline operator fun <reified T : JavaModifier> T.rangeTo(other: T) = arrayOf(this, other)

/**
 * Utility method to chain modifiers using the rangeTo operator.
 *
 * @since 1.0.0
 */
inline operator fun <reified T : JavaModifier> Array<T>.rangeTo(other: T) = arrayOf(*this, other)