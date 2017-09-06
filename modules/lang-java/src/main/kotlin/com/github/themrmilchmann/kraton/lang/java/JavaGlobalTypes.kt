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

/**
 * The `void` type.
 *
 * @since 1.0.0
 */
object void: JavaReferableType("void")

/** The `boolean` type.
 *
 * @since 1.0.0
 */
val boolean = JavaPrimitiveType("Boolean", "boolean", "false", -1)

/**
 * The `byte` type.
 *
 * @since 1.0.0
 */
val byte = JavaPrimitiveType("Byte", "byte", "0", 8)

/**
 * The `short` type.
 *
 * @since 1.0.0
 */
val short = JavaPrimitiveType("Short", "short", "0", 16)

/**
 * The `char` type.
 *
 * @since 1.0.0
 */
val char = JavaPrimitiveType("Character", "char", "'\\u0000'", 32, "Char")

/**
 * The `int` type.
 *
 * @since 1.0.0
 */
val int = JavaPrimitiveType("Integer", "int", "0", 32, "Int")

/**
 * The `float` type.
 *
 * @since 1.0.0
 */
val float = JavaPrimitiveType("Float", "float", "0F", 32)

/**
 * The `long` type.
 *
 * @since 1.0.0
 */
val long = JavaPrimitiveType("Long", "long", "0L", 64)

/**
 * The `double` type.
 *
 * @since 1.0.0
 */
val double = JavaPrimitiveType("Double", "double", "0D", 64)

/**
 * The `String` type.
 *
 * @since 1.0.0
 */
val string = JavaTypeReference("String", null)