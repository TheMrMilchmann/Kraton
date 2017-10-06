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

/**
 * An object representing a Java method.
 *
 * @property returnType     the return type of the method
 * @property name           the name of the method
 * @property documentation  the documentation of the method
 * @property parameters     the parameters of the method
 * @property returnDoc      the documentation of the method's `@returnDoc` tag
 * @property since          the documentation of the method's `@since` tag
 * @property category       the category under which this method will be
 *                          generated within it's container
 * @property exceptions     the exceptions that may be thrown by the method
 *                          and their respective documentation entries
 * @property see            the objects to be referenced in the method's
 *                          documentation
 * @property typeParameters the type parameters of the method
 * @property body           the body of the method
 *
 * @since 1.0.0
 */
open class JavaMethod internal constructor(
    val returnType: IJvmType,
    override val name: String,
    val documentation: String,
    val parameters: Array<out JavaParameter>,
    val returnDoc: String?,
    val since: String?,
    val exceptions: Array<out Pair<IJvmType, String?>>?,
    val see: Array<out String>?,
    val typeParameters: Array<out Pair<JvmGenericType, String?>>?,
    private val body: String?
) : JavaModifierTarget(), JavaBodyMember {

    override fun PrintWriter.printMember(indent: String, containerType: JavaTopLevelType<*, *>) {
        val documentation = toJavaDoc(indent, containerType)
        if (documentation != null) println(documentation)

        printAnnotations(indent, containerType)
        print(indent)
        printModifiers()

        if (typeParameters != null) {
            print("<")
            print(StringJoiner(", ").run {
                typeParameters.forEach { add(it.first.asString(containerType)) }
                toString()
            })
            print("> ")
        }

        printMethodHead(containerType)
        print("(")

        if (parameters.isNotEmpty()) {
            print(StringJoiner(", ").apply {
                parameters.forEach {
                    add("${it.printAnnotationsInline(containerType)}${it.type.asString(containerType)} ${it.name}")
                }
            })
        }

        print(")")

        if (exceptions != null) {
            print(" throws ")
            print(StringJoiner(", ").apply {
                exceptions.forEach {
                    add(it.first.asString(containerType))
                }
            })
        }

        if (body == null) {
            println(";")
        } else {
            print(" {")

            if (body.isNotEmpty()) {
                var body: String = body

                while (body.startsWith(LN)) body = body.removePrefix(LN)
                while (body.endsWith(LN)) body = body.removeSuffix(LN)

                println()
                body.lineSequence().forEach {
                    print(indent + INDENT)
                    println(it)
                }

                print(indent)
            }

            println("}")
        }

        println()
    }

    /**
     * Prints the head of this method.
     *
     * @receiver the PrintWriter to print to
     *
     * @since 1.0.0
     */
    internal open fun PrintWriter.printMethodHead(containerType: JavaTopLevelType<*, *>) {
        print(returnType.asString(containerType))
        print(" ")
        print(name)
    }

}

/**
 * A java constructor.
 *
 * @constructor Creates a new JavaConstructor with the given properties.
 *
 * @since 1.0.0
 */
class JavaConstructor internal constructor(
    returnType: IJvmType,
    documentation: String,
    parameters: Array<out JavaParameter>,
    returnDoc: String?,
    since: String?,
    exceptions: Array<out Pair<IJvmType, String?>>?,
    see: Array<out String>?,
    typeParameters: Array<out Pair<JvmGenericType, String?>>?,
    body: String?
) : JavaMethod(returnType, returnType.className, documentation, parameters, returnDoc, since, exceptions, see, typeParameters, body) {

    /**
     * Prints the head of this constructor.
     *
     * @receiver the PrintWriter to print to
     *
     * @since 1.0.0
     */
    override fun PrintWriter.printMethodHead(containerType: JavaTopLevelType<*, *>) {
        print(returnType.asString(containerType))
    }

}