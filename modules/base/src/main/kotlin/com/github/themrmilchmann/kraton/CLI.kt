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
package com.github.themrmilchmann.kraton

internal const val CLI_HEADER = """
|------------------------------------------------------------------------------|
|                                    Kraton                                    |
|                      Copyright (c) 2017 Leon Linhart                         |
|                                                                              |
| More information:                                                            |
| https://github.com/TheMrMilchmann/Kraton                                     |
|------------------------------------------------------------------------------|
"""

internal const val CLI_HELP = """
The generator must be started with at least two arguments:

1) the root directory of the templates' sources, and
2) the target location for the generated code.

Additionally the generator's behaviour may be altered by using the following CLI
options:

|------------------------------------------------------------------------------|
| Short | Long                | Description                                    |
|------------------------------------------------------------------------------|
| -f      --force               By default the generator uses incremental      |
|                               generation (a feature similar to incremental   |
|                               compilation) to reduce the required execution  |
|                               time.                                          |
|                               If this option is used, the generator does not |
|                               perform those checks.                          |
|                                                                              |
| -g      --generator-source    This option may be specified to make the       |
|                               generator also take possible updates of itself |
|                               into consideration when using incremental      |
|                               generation.                                    |
|                                                                              |
| -h      --help                Print this information. (Exits the process)    |
|                                                                              |
| -v      --version             Print the version information.                 |
|                               (Exits the process)                            |
|------------------------------------------------------------------------------|
"""

internal fun cliOut(msg: String) {
    var out = msg

    while (out.startsWith('\n')) out = out.removePrefix("\n")
    while (out.endsWith('\n')) out = out.removeSuffix("\n")

    print(out)
}

internal fun cliOutln(msg: String) {
    cliOut(msg)
    println()
}