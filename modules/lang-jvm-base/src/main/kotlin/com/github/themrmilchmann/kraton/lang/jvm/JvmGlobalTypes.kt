/*
 * Copyright (c) 2017 Leon Linhart,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
package com.github.themrmilchmann.kraton.lang.jvm

/**
 * The `void` type.
 *
 * @since 1.0.0
 */
@JvmField
val void = object : AbstractJvmType("void") {

    override fun nullable() =
        object : AbstractJvmType("void") {
            override val isNullable = true
            override fun nullable() = this
        }

}

/**
 * The `void` type.
 *
 * @since 1.0.0
 */
@JvmField val void_t = void

/** The `boolean` type.
 *
 * @since 1.0.0
 */
@JvmField val boolean = JvmPrimitiveType("Boolean", "false", -1)

/** The `boolean` type.
 *
 * @since 1.0.0
 */
@JvmField val boolean_t = boolean

/**
 * The `byte` type.
 *
 * @since 1.0.0
 */
@JvmField val byte = JvmPrimitiveType("Byte", "0", 8)
@JvmField val byte_t = byte

/**
 * The `short` type.
 *
 * @since 1.0.0
 */
@JvmField val short = JvmPrimitiveType("Short", "0", 16)

/**
 * The `short` type.
 *
 * @since 1.0.0
 */
@JvmField val short_t = short

/**
 * The `char` type.
 *
 * @since 1.0.0
 */
@JvmField val char = JvmPrimitiveType("Character", "'\\u0000'", 32, "Char")

/**
 * The `char` type.
 *
 * @since 1.0.0
 */
@JvmField val char_t = char

/**
 * The `int` type.
 *
 * @since 1.0.0
 */
@JvmField val int = JvmPrimitiveType("Integer", "0", 32, "Int")

/**
 * The `int` type.
 *
 * @since 1.0.0
 */
@JvmField val int_t = int

/**
 * The `float` type.
 *
 * @since 1.0.0
 */
@JvmField val float = JvmPrimitiveType("Float", "0F", 32)

/**
 * The `float` type.
 *
 * @since 1.0.0
 */
@JvmField val float_t = float

/**
 * The `long` type.
 *
 * @since 1.0.0
 */
@JvmField val long = JvmPrimitiveType("Long", "0L", 64)

/**
 * The `long` type.
 *
 * @since 1.0.0
 */
@JvmField val long_t = long

/**
 * The `double` type.
 *
 * @since 1.0.0
 */
@JvmField val double = JvmPrimitiveType("Double", "0D", 64)

/**
 * The `double` type.
 *
 * @since 1.0.0
 */
@JvmField val double_t = double

/**
 * The `String` type.
 *
 * @since 1.0.0
 */
@JvmField val string = JvmTypeReference("String", "java.lang")