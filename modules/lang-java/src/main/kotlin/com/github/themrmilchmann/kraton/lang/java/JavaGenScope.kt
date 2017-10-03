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
package com.github.themrmilchmann.kraton.lang.java

import com.github.themrmilchmann.kraton.lang.jvm.*
import java.io.*

abstract class JavaScope<T: JavaTopLevelType<T, S>, S: JavaScope<T, S>>(
    val scopeRoot: T,
    val members: MutableSet<JavaBodyMember>
) {

    /**
     * Imports all types that are directly underneath the given `type` using
     * a wildcard import.
     *
     * @param type
     * @param isStatic
     * @param isImplicit
     *
     * @since 1.0.0
     */
    @JvmOverloads
    fun import(type: String, isStatic: Boolean = false, isImplicit: Boolean = false) =
        scopeRoot.import(type, isStatic, isImplicit)

    /**
     * Imports the given type.
     *
     * When no import mode is forced (= `forceMode` is `null`), the import may
     * be a wildcard import depending on the configuration.
     *
     * @param type          the type to import
     * @param forceMode     the
     * @param isImplicit    whether or not the import is implicit (implicit
     *                      imports are not printed)
     *
     * @since 1.0.0
     */
    @JvmOverloads
    fun import(
        type: IJvmType,
        forceMode: JavaImportForceMode? = null,
        isImplicit: Boolean = false
    ) = type.packageName?.let { scopeRoot.import(type, forceMode, isImplicit) }

    /**
     * Statically imports the given member type.
     *
     * This should be used for methods and fields only.
     *
     * When no import mode is forced (= `forceMode` is `null`), the import may
     * be a wildcard import depending on the configuration.
     *
     * @param type          the type to import
     * @param member        the member to import
     * @param forceMode     the
     * @param isImplicit    whether or not the import is implicit (implicit
     *                      imports are not printed)
     *
     * @since 1.0.0
     */
    @JvmOverloads
    fun import(
        type: IJvmType,
        member: String,
        forceMode: JavaImportForceMode? = null,
        isImplicit: Boolean = false
    ) = type.packageName?.let { scopeRoot.import(type, member, forceMode, isImplicit) }

    /**
     * Creates and registers a new group. For Java code a group is a solely
     * theoretical construct that may be used to group elements logically.
     *
     * @param name              the name for the group
     * @param sortingStrategy   the sorting strategy used for members within the
     *                          group (may be `null` to retain the order in
     *                          which the elements were added)
     * @param headerPrinter     a group may be prefixed with a header by using
     *                          this parameter
     *
     * @since 1.0.0
     */
    @JvmOverloads
    fun group(
        name: String = "",
        sortingStrategy: Comparator<JavaBodyMember>? = null,
        headerPrinter: (PrintWriter.(indent: String, group: JavaBodyMemberGroup<*, *>) -> Unit)? = null,
        init: S.() -> Unit
    ) = JavaBodyMemberGroup(scopeRoot, name, sortingStrategy, headerPrinter, init)
        .also { members.add(it) }

    /**
     * Creates, registers and returns an object representing a Java class.
     *
     * @receiver the enclosing type of which the class will be a member of
     *
     * @param className     the name for the class
     * @param documentation the documentation for the class
     * @param since         the value for the class' `@since` tag
     * @param superClass    the parent class for the class
     * @param interfaces    the interfaces for the class to implement
     *
     * @return the newly created and registered JavaClass object
     *
     * @since 1.0.0
     */
    @JvmOverloads
    fun javaClass(
        className: String,
        documentation: String? = null,
        since: String? = null,
        superClass: IJvmType? = null,
        interfaces: Array<out IJvmType>? = null,
        init: JavaClassScope.() -> Unit
    ) = JavaClass(className, scopeRoot.packageName, documentation, since, superClass, interfaces, scopeRoot)
        .apply { JavaClassScope(this, members).also(init) }
        .also { members.add(it) }

    /**
     * Creates, registers and returns an object representing a Java interface.
     *
     * @receiver the enclosing type of which the interface will be a member of
     *
     * @param className         the name for the interface
     * @param documentation     the documentation for the interface
     * @param since             the value for the interface's `@since` tag
     * @param superInterfaces   the interfaces for the interface to extend
     *
     * @return the newly created and registered JavaInterface object
     *
     * @since 1.0.0
     */
    @JvmOverloads
    fun javaInterface(
        className: String,
        documentation: String? = null,
        since: String? = null,
        superInterfaces: Array<out IJvmType>? = null,
        init: JavaInterfaceScope.() -> Unit
    ) = JavaInterface(className, scopeRoot.packageName, documentation, since, superInterfaces, scopeRoot)
        .apply { JavaInterfaceScope(this, members).also(init) }
        .also { members.add(it) }

}