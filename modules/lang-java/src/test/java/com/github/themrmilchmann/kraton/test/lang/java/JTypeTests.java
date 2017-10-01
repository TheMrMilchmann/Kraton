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
package com.github.themrmilchmann.kraton.test.lang.java;

import com.github.themrmilchmann.kraton.lang.java.*;
import com.github.themrmilchmann.kraton.lang.jvm.IJvmType;
import com.github.themrmilchmann.kraton.lang.jvm.JvmTypesKt;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public final class JTypeTests {

    @Test
    public void testJavaClassAsType() {
        IJvmType type = JvmTypesKt.asType(Object.class);

        assertEquals(type.getClassName(), "Object");
        assertEquals(type.getPackageName(), "java.lang");
    }

    @Test
    public void testJavaInnerClassAsType() {
        IJvmType type = JvmTypesKt.asType(java.util.Map.Entry.class);

        assertEquals(type.getClassName(), "Entry");
        assertEquals(type.getMemberName(), "Map.Entry");
        assertEquals(type.getContainerName(), "Map");
        assertEquals(type.getPackageName(), "java.util");
        assertEquals(JavaTypesKt.asString(type, null), "java.util.Map.Entry");
    }

    @Test
    public void testJavaClassWithParamsAsType() {
        IJvmType typeParam = JvmTypesKt.asType(String.class);
        IJvmType type = JvmTypesKt.asType(java.util.List.class, typeParam);

        assertEquals(type.getClassName(), "List");
        assertEquals(type.getPackageName(), "java.util");
        assertEquals(JavaTypesKt.asString(type, null), "java.util.List<java.lang.String>");
    }

}