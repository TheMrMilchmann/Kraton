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
package com.github.themrmilchmann.kraton.test.lang.java

import com.github.themrmilchmann.kraton.lang.java.*

private const val packageName = "com.github.themrmilchmann.kraton.test.lang.java.imports"
private const val srcFolder = "lang-java/build"
private const val srcSet = "kraton/generated"

// Types used to test the imports
private val file = JavaTypeReference("File", "java.io")
private val filereader = JavaTypeReference("FileReader", "java.io")
private val fis = JavaTypeReference("FileInputStream", "java.io")
private val fos = JavaTypeReference("FileOutputStream", "java.io")
private val ioe = JavaTypeReference("IOException", "java.io")
private val eof = JavaTypeReference("EOFException", "java.io")

val Imports = Profile {

    // Test imports in classes

    public..final..javaClass(
        "I2FN1FWClass",
        packageName,
        srcFolder,
        srcSet
    ) {
        import(ioe, JavaImportForceMode.FORCE_WILDCARD)

        file(
            "test",
            "",

            exceptions = arrayOf(
                eof to null,
                ioe to null
            ),
            body = "return null;"
        )

    }

    public..final..javaClass(
        "I1FQ1FWClass",
        packageName,
        srcFolder,
        srcSet
    ) {
        import(file, JavaImportForceMode.FORCE_QUALIFIED)
        import(eof, JavaImportForceMode.FORCE_WILDCARD)

        file(
            "test",
            "",

            exceptions = arrayOf(
                eof to null,
                ioe to null
            ),
            body = "return null;"
        )

    }

    public..final..javaClass(
        "I3FQ1FNClass",
        packageName,
        srcFolder,
        srcSet
    ) {
        import(file, JavaImportForceMode.FORCE_QUALIFIED)
        import(eof, JavaImportForceMode.FORCE_QUALIFIED)
        import(ioe, JavaImportForceMode.FORCE_QUALIFIED)

        file(
            "test",
            "",

            filereader.PARAM("par0", ""),

            exceptions = arrayOf(
                eof to null,
                ioe to null
            ),
            body = "return null;"
        )

    }

    public..final..javaClass(
        "I3FQ3FNClass",
        packageName,
        srcFolder,
        srcSet
    ) {
        import(file, JavaImportForceMode.FORCE_QUALIFIED)
        import(eof, JavaImportForceMode.FORCE_QUALIFIED)
        import(ioe, JavaImportForceMode.FORCE_QUALIFIED)

        file(
            "test",
            "",

            filereader.PARAM("par0", ""),
            fis.PARAM("par1", ""),
            fos.PARAM("par2", ""),

            exceptions = arrayOf(
                eof to null,
                ioe to null
            ),
            body = "return null;"
        )

    }

    // Test imports in interfaces

    public..final..javaInterface(
        "I2FN1FWInterface",
        packageName,
        srcFolder,
        srcSet
    ) {
        import(ioe, JavaImportForceMode.FORCE_WILDCARD)

        file(
            "test",
            "",

            exceptions = arrayOf(
                eof to null,
                ioe to null
            )
        )

    }

    public..final..javaInterface(
        "I1FQ1FWInterface",
        packageName,
        srcFolder,
        srcSet
    ) {
        import(file, JavaImportForceMode.FORCE_QUALIFIED)
        import(eof, JavaImportForceMode.FORCE_WILDCARD)

        file(
            "test",
            "",

            exceptions = arrayOf(
                eof to null,
                ioe to null
            )
        )

    }

    public..final..javaInterface(
        "I3FQ1FNInterface",
        packageName,
        srcFolder,
        srcSet
    ) {
        import(file, JavaImportForceMode.FORCE_QUALIFIED)
        import(eof, JavaImportForceMode.FORCE_QUALIFIED)
        import(ioe, JavaImportForceMode.FORCE_QUALIFIED)

        file(
            "test",
            "",

            filereader.PARAM("par0", ""),

            exceptions = arrayOf(
                eof to null,
                ioe to null
            )
        )

    }

    public..final..javaInterface(
        "I3FQ3FNInterface",
        packageName,
        srcFolder,
        srcSet
    ) {
        import(file, JavaImportForceMode.FORCE_QUALIFIED)
        import(eof, JavaImportForceMode.FORCE_QUALIFIED)
        import(ioe, JavaImportForceMode.FORCE_QUALIFIED)

        file(
            "test",
            "",

            filereader.PARAM("par0", ""),
            fis.PARAM("par1", ""),
            fos.PARAM("par2", ""),

            exceptions = arrayOf(
                eof to null,
                ioe to null
            )
        )

    }

    // Test imports in nested types
    // NOTE: This is just a simple test to see if the redirection is working properly.

    public..final..javaClass(
        "I1FNNested",
        packageName,
        srcFolder,
        srcSet
    ) {

        javaClass(
            "NestedClass"
        ) {

            file(
                "test",
                "",

                body = "return null;"
            )

        }

    }

}