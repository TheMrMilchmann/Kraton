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
package com.github.themrmilchmann.kraton.cli

import com.github.themrmilchmann.kopt.*
import com.github.themrmilchmann.kraton.io.*
import com.github.themrmilchmann.kraton.tools.*
import kotlin.system.*

private const val NO_SUCH_TOOL_ERROR    = -1000
private const val CLI_PARSING_ERROR     = -2000

private val logger = System.out.logger

fun main(args: Array<String>) {
    val res: Int = when (args[0]) {
        "generator", "generate" -> KGenerator(logger)
        else -> null
    }.let { it?.resolveAndExec(args) ?: toolLaunchError(args[0]) }

    exitProcess(res)
}

private fun <C> AbstractTool<C>.resolveAndExec(args: Array<String>): Int {
    return try {
        val cfg = parseCfg(args)
        exec(cfg)
    } catch (e: ParsingException) {
        logger.error(e.message)
        CLI_PARSING_ERROR
    }
}

private fun toolLaunchError(tool: String): Int {
    logger.error("Tool '$tool' is not available")
    return NO_SUCH_TOOL_ERROR
}