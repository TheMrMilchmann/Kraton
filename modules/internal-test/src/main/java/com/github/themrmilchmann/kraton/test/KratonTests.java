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
package com.github.themrmilchmann.kraton.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.testng.ITest;
import org.testng.TestException;
import org.testng.TestNGException;
import org.testng.annotations.*;

public final class KratonTests {

    @Factory
    @Parameters({"rootDir", "module"})
    public Object[] generateLangModuleTests(String rootDir, String module) {
        Path projectRoot = Paths.get(rootDir, "/modules/");
        List<String> testOutputFiles = new CopyOnWriteArrayList<>();
        List<String> testResultFiles = new CopyOnWriteArrayList<>();

        Path moduleDir = projectRoot.resolve("lang-" + module);
        Path testSourceDir = moduleDir.resolve("src/test-integration/kotlin");
        if (!Files.exists(testSourceDir)) return new Object[] {};

        Path testOutputDir = moduleDir.resolve("build/kraton/generated");
        if (!Files.exists(testOutputDir)) {
            return new Object[] { new ITest() {

                @Override
                public String getTestName() {
                    return "NoTestOutputDir";
                }

                @Test
                public void test() throws IOException {
                    throw new TestNGException(testOutputDir.toAbsolutePath() + " does not exist!");
                }

            }};
        }

        Path testResultDir = moduleDir.resolve("src/test-integration/resources");
        List<Object> tests = new ArrayList<>(16);

        try {
            Files.walkFileTree(testOutputDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    if (Files.isRegularFile(file)) {
                        testOutputFiles.add(testOutputDir.relativize(file).toString());
                    }

                    return FileVisitResult.CONTINUE;
                }

            });

            Files.walkFileTree(testResultDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    if (Files.isRegularFile(file)) {
                        testResultFiles.add(testResultDir.relativize(file).toString());
                    }

                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String testFileName : testOutputFiles) {
            testOutputFiles.remove(testFileName);

            if (testResultFiles.contains(testFileName)) {
                testResultFiles.remove(testFileName);

                tests.add(new ITest() {

                    @Override
                    public String getTestName() {
                        return testFileName;
                    }

                    @Test
                    public void test() throws IOException {
                        BufferedReader testOutputReader = Files.newBufferedReader(testOutputDir.resolve(testFileName));
                        BufferedReader testResultReader = Files.newBufferedReader(testResultDir.resolve(testFileName));

                        String testOutputLine,
                            testResultLine = null;
                        int line = 0;

                        while (((testOutputLine = testOutputReader.readLine()) != null) && ((testResultLine = testResultReader.readLine()) != null)) {
                            line++;

                            if (!testOutputLine.contentEquals(testResultLine)) {
                                throw new TestException(String.format("Line %d of output file '%s' does not match the expected result.", line, testFileName));
                            }
                        }

                        if (testOutputLine != null) {
                            throw new TestException(String.format("The output file '%s' contains more lines than expected.", testFileName));
                        } else if (testResultReader.readLine() != null) {
                            throw new TestException(String.format("The output file '%s' is shorter than expected.", testFileName));
                        }
                    }

                });
            } else {
                tests.add(new ITest() {

                    @Override
                    public String getTestName() {
                        return testFileName;
                    }

                    @Test
                    public void test() {
                        throw new TestException(String.format("The output file '%s' was generated unexpectedly.", testFileName));
                    }

                });
            }
        }

        for (String testResultFile : testResultFiles) {
            tests.add(new ITest() {

                @Override
                public String getTestName() {
                    return testResultFile;
                }

                @Test
                public void test() {
                    throw new TestException(String.format("The expected output file '%s' has not been generated.", testResultFile));
                }

            });
        }

        return tests.toArray();
    }

}