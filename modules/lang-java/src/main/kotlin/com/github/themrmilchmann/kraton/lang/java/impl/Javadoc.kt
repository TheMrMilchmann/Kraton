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
package com.github.themrmilchmann.kraton.lang.java.impl

import com.github.themrmilchmann.kraton.io.*
import com.github.themrmilchmann.kraton.lang.jvm.IJvmType

internal class Documentation {

    val authors: MutableList<String> = mutableListOf()
    val params: MutableMap<FormalParameter, String> = mutableMapOf()
    val typeParams: MutableMap<TypeParameter, String> = mutableMapOf()
    var content: String? = null
    var returnDoc: String? = null
    val exceptions: MutableMap<IJvmType, String> = mutableMapOf()
    val see: MutableList<String> = mutableListOf()
    var since: String? = null

    fun JavaPrinter.print(scope: OrdinaryCompilationUnit?) {
        if (authors.isNotEmpty()
            || params.any { it.value.isNotEmpty() }
            || typeParams.any { it.value.isNotEmpty() }
            || content !== null
            || returnDoc !== null
            || exceptions.any { it.value.isNotEmpty() }
            || see.isNotEmpty()
            || since !== null) {
            print(StringBuilder().apply {
                content?.let { append(it.cleanup("$indent * ")) }

                if (typeParams.any { it.value.isNotEmpty() }) {
                    if (isNotEmpty()) append("$ln$indent *")
                    typeParams.filter { it.value.isNotEmpty() }.forEach { key, value ->
                        if (isNotEmpty()) append("$ln$indent * ")
                        append("@param <${key.identifier}> $value")
                    }
                }

                if (params.any { it.value.isNotEmpty() }) {
                    if (isNotEmpty()) append("$ln$indent *")
                    params.filter { it.value.isNotEmpty() }.forEach { key, value ->
                        if (isNotEmpty()) append("$ln$indent * ")
                        append("@param <${key.name}> $value")
                    }
                }

                returnDoc?.let {
                    if (isNotEmpty()) append("$ln$indent *$ln$indent *")
                    append("@return $it")
                }

                if (exceptions.any { it.value.isNotEmpty() }) {
                    if (isNotEmpty()) append("$ln$indent *")
                    typeParams.filter { it.value.isNotEmpty() }.forEach { key, value ->
                        if (isNotEmpty()) append("$ln$indent * ")
                        append("@throws ${key.identifier} $value")
                    }
                }

                if (see.isNotEmpty()) {
                    if (isNotEmpty()) append("$ln$indent *")
                    see.forEach {
                        if (isNotEmpty()) append("$ln$indent * ")
                        append("@see $it$")
                    }
                }

                since?.let {
                    if (isNotEmpty()) append("$ln$indent *$ln$indent * ")
                    append("@since $it")
                }

                if (authors.isNotEmpty()) {
                    if (isNotEmpty()) append("$ln$indent *")
                    authors.forEach {
                        if (isNotEmpty()) append("$ln$indent * ")
                        append("@author $it")
                    }
                }
            }.toString().layoutJavadoc(indent) + ln)
        }

    }

}

/*
 * The following logic is borrowed from LWJGL's code generation.
 * (lwjgl.org, github.com/LWJGL/lwjgl3)
 *
 * A copy of LWJGL's license is available in the projects LICENSE file.
 */

/*
Here we perform the following transformation:
<block>                 <block>
text                    text
text            =>      <p>text</p>
text                    <p>text</p>
    <div>                   <div>
    text                    text
    text                    <p>text</p>
    </div>                  </div>
text                    <p>text</p>
</block>                </block>
The first text sub-block is not wrapped in <p> because:
    a) It is not strictly necessary, renders fine in browsers and IDEs.
    b) It improves readability of the source javadoc.
For the purposes of this transformation, the javadoc root is an implicit block.
 */

private val REDUNDANT_WHITESPACE = "^[ \\t]+$".toRegex(RegexOption.MULTILINE)
private val BLOCK_NODE = "(?:div|h[1-6]|code(?=><pre>)|(?<=</pre></)code|table|thead|tfoot|tbody|td|tr|ul|li|ol|dl|dt|dd)" // TODO: add more here if necessary
private val FRAGMENT = "(</?$BLOCK_NODE(?:\\s[^>]+)?>|^)([\\s\\S]*?)(?=</?$BLOCK_NODE(?:\\s[^>]+)?>|$)".toRegex()
private val CHILD_NODE = "<(?:tr|thead|tfoot|tbody|li|dt|dd)>".toRegex()
private val PARAGRAPH_PATTERN = "\\n\\n(?:\\n?[ \\t]*[\\S][^\\n]*)+".toRegex(RegexOption.MULTILINE)
private val PREFIX_PATTERN = "^(?:\uFFFF|[ \t]++(?![*]))".toRegex(RegexOption.MULTILINE)

private fun String.cleanup(linePrefix: String = "$FOUR_SPACES * "): String {
    val dom = trim().replace(REDUNDANT_WHITESPACE, "")
    return StringBuilder(dom.length)
        .layoutDOM(dom, linePrefix)
        .replace(PREFIX_PATTERN, linePrefix)
}

private fun StringBuilder.layoutDOM(dom: String, linePrefix: String): StringBuilder {
    FRAGMENT.findAll(dom).forEach { match ->
        val (tag, text) = match.destructured

        if (tag.isNotEmpty()) {
            if (startNewLine(dom, match.range.start)) {
                if (!tag.startsWith("</") && !tag.matches(CHILD_NODE)) {
                    append('\n')
                    append(linePrefix)
                }
                append('\n')
                append(linePrefix)
            }
            append(tag)
        }

        text.trim().let {
            if (it.isNotEmpty())
                layoutText(it, linePrefix, forceParagraph = tag.isNotEmpty() && tag.startsWith("</"))
        }
    }

    return this
}

private fun startNewLine(dom: String, index: Int): Boolean {
    if (index == 0)
        return false

    for (i in (index - 1) downTo 0) {
        if (dom[i] == '\n')
            return true

        if (!dom[i].isWhitespace())
            break
    }

    return false
}

private fun StringBuilder.layoutText(text: String, linePrefix: String, forceParagraph: Boolean = false) {
    var to: Int = -1

    PARAGRAPH_PATTERN.findAll(text).forEach { match ->
        val from = match.range.start
        if (to == -1 && from > 0)
            appendParagraphFirst(linePrefix, text, from, forceParagraph)

        to = match.range.endInclusive + 1
        appendParagraph(linePrefix, text, from, to)
    }

    if (to == -1)
        appendParagraphFirst(linePrefix, text, text.length, forceParagraph)
    else if (to < text.length)
        appendParagraph(linePrefix, text, to, text.length)
}

private fun StringBuilder.appendParagraphFirst(linePrefix: String, text: String, end: Int, forceParagraph: Boolean = false) {
    if (forceParagraph)
        appendParagraph(linePrefix, text, 0, end)
    else
        append(text, 0, end)
}

private fun StringBuilder.appendParagraph(linePrefix: String, text: String, start: Int, end: Int) {
    append('\n')
    append(linePrefix)
    append('\n')
    append(linePrefix)

    append("<p>")
    append(text.substring(start, end).trim())
    append("</p>")
}

private fun String.layoutJavadoc(indentation: String = FOUR_SPACES): String {
    return if (this.indexOf('\n') == -1)
        "$indentation/** $this */"
    else
        "$indentation/**\n$indentation * $this\n$indentation */"
}