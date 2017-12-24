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

import com.github.themrmilchmann.kraton.lang.jvm.*

/**
 * Casts a value to another type (excluding unnecessary casts).
 *
 * @param from  the initial type
 * @param to    the desired type
 * @param value the value to be cast
 *
 * @return the casted value
 *
 * @since 1.0.0
 */
fun cast(from: AbstractJvmType, to: AbstractJvmType, value: String): String {
    if (from is JvmPrimitiveType && to is JvmPrimitiveType) {
        when (to) {
            byte  -> when (from) {
                short, int, char, long -> return "($to) $value"
            }
            short -> when (from) {
                int, char, long -> return "($to) $value"
            }
            int   -> when (from) {
                long, float, double -> return "($to) $value"
            }
            char  -> when (from) {
                byte, short, int, long, float, double -> return "($to) $value"
            }
            long  -> when (from) {
                float, double -> return "($to) $value"
            }
            float -> when (from) {
                double -> return "($to) $value"
            }
        }

        return value
    }

    return "($to) $value"
}

/**
 * Converts a value to another type.
 *
 * @param from  the initial type
 * @param to    the desired type
 * @param value the value to be converted
 *
 * @return the converted value
 *
 * @since 1.0.0
 */
fun convert(from: AbstractJvmType, to: AbstractJvmType, value: String): String {
    if (from is JvmPrimitiveType && to is JvmPrimitiveType) {
        when (from) {
            boolean -> when (to) {
                byte, short, int, float, double, long -> return cast(int, to, "($value ? 1 : ${to.nullValue})")
                char                                  -> return "$value ? '\\u0001' : ${to.nullValue}"
            }
            float   -> when (to) {
                byte, short -> return convert(int, to, "Float.floatToRawIntBits($value)")
                int, long   -> return "Float.floatToRawIntBits($value)"
            }
            double  -> when (to) {
                byte, short, int -> return convert(long, to, "Double.doubleToRawLongBits($value)")
                long             -> return "Double.doubleToRawLongBits($value)"
            }
        }

        when (to) {
            boolean -> return "$value != ${from.nullValue}"
            float   -> when (from) {
                byte, short, int, char -> return "Float.intBitsToFloat($value)"
            }
            double  -> when (from) {
                byte, short, int, char, long -> return "Double.longBitsToDouble($value)"
            }
        }
    }

    return cast(from, to, value)
}