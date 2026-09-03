/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.test;

import io.sbk.config.Config;
import io.sbk.utils.SbkUtils;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class SbkUtilsTest {

    /** Parse valid boolean spellings and reject ambiguous values. */
    @Test
    public void parsesBooleanOptionsStrictly() {
        assertTrue(SbkUtils.parseBooleanOption("flag", "TRUE"));
        assertFalse(SbkUtils.parseBooleanOption("flag", "false"));
        assertThrows(IllegalArgumentException.class,
                () -> SbkUtils.parseBooleanOption("flag", "yes"));
    }

    @Test
    public void testRemoveOptionArgsAndValues() {
        String[] args = {"-a", "1", "-b", "2", "-c", "3"};
        String[] opts = {"-a", "-c"};
        String[] result = SbkUtils.removeOptionArgsAndValues(args, opts);
        assertArrayEquals(new String[]{"-b", "2"}, result);

        assertArrayEquals(new String[0], SbkUtils.removeOptionArgsAndValues(null, opts));
        assertArrayEquals(new String[0], SbkUtils.removeOptionArgsAndValues(new String[]{"-a"}, opts));
    }

    @Test
    public void testRemoveOptionArgs() {
        String[] args = {"-a", "1", "-b", "2", "-c", "3"};
        String[] opts = {"-a", "-c"};
        String[] result = SbkUtils.removeOptionArgs(args, opts);
        assertArrayEquals(new String[]{"1", "-b", "2", "3"}, result);

        assertArrayEquals(new String[0], SbkUtils.removeOptionArgs(null, opts));
        assertArrayEquals(new String[]{"-a"}, SbkUtils.removeOptionArgs(new String[]{"-a"}, new String[]{"-b"}));
    }

    @Test
    public void testGetArgValue() {
        String[] args = {"-a", "foo", "-b", "bar"};
        assertEquals("foo", SbkUtils.getArgValue(args, "-a"));
        assertEquals("bar", SbkUtils.getArgValue(args, "-b"));
        assertEquals("", SbkUtils.getArgValue(args, "-c"));
        assertEquals("", SbkUtils.getArgValue(new String[]{"-a"}, "-a"));
        assertEquals("", SbkUtils.getArgValue(null, "-a"));
    }

    @Test
    public void testGetClassNameAndLoggerName() {
        String[] args = {Config.CLASS_OPTION_ARG, "MyClass", Config.LOGGER_OPTION_ARG, "MyLogger"};
        assertEquals("MyClass", SbkUtils.getClassName(args));
        assertEquals("MyLogger", SbkUtils.getLoggerName(args));
    }

    @Test
    public void testHasArg() {
        String[] args = {"-a", "foo", "-b"};
        assertTrue(SbkUtils.hasArg(args, "-a"));
        assertTrue(SbkUtils.hasArg(args, "-b"));
        assertFalse(SbkUtils.hasArg(args, "-c"));
        assertFalse(SbkUtils.hasArg(null, "-a"));
    }

    @Test
    public void testHasHelp() {
        String[] args = {Config.HELP_OPTION_ARG, "foo"};
        assertTrue(SbkUtils.hasHelp(args));
        assertFalse(SbkUtils.hasHelp(new String[]{"-a"}));
    }

    @Test
    public void testMapToArgs() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        String[] args = SbkUtils.mapToArgs(map, true);
        assertEquals(4, args.length);
        assertTrue(args[0].startsWith(Config.ARG_PREFIX));
        assertTrue(args[2].startsWith(Config.ARG_PREFIX));
    }

    @Test
    public void testArgsToMap() {
        String[] args = {"-a", "1", "-b", "2"};
        Map<String, String> map = SbkUtils.argsToMap(args, true);
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));

        Map<String, String> map2 = SbkUtils.argsToMap(args, false);
        assertEquals("1", map2.get("-a"));
        assertEquals("2", map2.get("-b"));
    }

    @Test
    public void testMergeArgs() {
        String[] s1 = {"-a", "1", "-b", "2"};
        String[] s2 = {"-b", "3", "-c", "4"};
        String[] merged = SbkUtils.mergeArgs(s1, s2);
        assertArrayEquals(new String[]{"-a", "1", "-b", "3", "-c", "4"}, merged);
        Map<String, String> map = SbkUtils.argsToMap(merged, false);
        assertEquals("1", map.get("-a"));
        assertEquals("3", map.get("-b"));
        assertEquals("4", map.get("-c"));
    }

    @Test
    public void testMergeArgsNormalizesOptionsAndUsesLastOverride() {
        String[] yamlArgs = {"-class", "file", "-size", "100", "-readers", "1"};
        String[] cliArgs = {"--size", "200", "-seconds", "60", "-size", "300"};

        assertArrayEquals(new String[]{"-class", "file", "-size", "300", "-readers", "1",
                "-seconds", "60"}, SbkUtils.mergeArgs(yamlArgs, cliArgs));
    }

    @Test
    public void testMergeArgsAcceptsMissingInputs() {
        assertArrayEquals(new String[]{"-writers", "2"},
                SbkUtils.mergeArgs(null, new String[]{"-writers", "2"}));
        assertArrayEquals(new String[]{"-writers", "1"},
                SbkUtils.mergeArgs(new String[]{"-writers", "1"}, null));
    }

    @Test
    public void testRedactOptionValues() {
        final String[] args = {"-gemuser", "root", "-gempass", "secret", "--size", "100",
                "--GEMPASS=second-secret"};

        assertArrayEquals(new String[]{"-gemuser", "root", "-gempass", "******", "--size", "100",
                "--GEMPASS=******"}, SbkUtils.redactOptionValues(args, new String[]{"gempass"}));
        assertEquals("secret", args[3]);
    }

    @Test
    public void testRedactSensitiveOptionValues() {
        final String[] args = {"-class", "minio", "-key", "access",
                "-secret", "secret", "--password=database-password",
                "-size", "100"};

        assertArrayEquals(new String[]{"-class", "minio", "-key", "******",
                "-secret", "******", "--password=******", "-size", "100"},
                SbkUtils.redactSensitiveOptionValues(args));
    }

    @Test
    public void testGetOperatingSystemDetails() {
        assertEquals(System.getProperty("os.name") + " " + System.getProperty("os.version")
                        + " (" + System.getProperty("os.arch") + ")",
                SbkUtils.getOperatingSystemDetails());
    }

    @Test
    public void testJavaSelectionSourceUsesLauncherProvenance() {
        assertEquals("SBK_JAVA_HOME environment variable",
                SbkUtils.getJavaSelectionSource(Map.of("SBK_JAVA_SOURCE", "SBK_JAVA_HOME"), "jdk-home"));
        assertEquals("JAVA_HOME environment variable",
                SbkUtils.getJavaSelectionSource(Map.of("SBK_JAVA_SOURCE", "JAVA_HOME"), "jdk-home"));
        assertEquals("system PATH",
                SbkUtils.getJavaSelectionSource(Map.of("SBK_JAVA_SOURCE", "PATH"), "jdk-home"));
        assertEquals("SBK managed JDK cache",
                SbkUtils.getJavaSelectionSource(Map.of("SBK_JAVA_SOURCE", "SBK_MANAGED_JDK_CACHE"), "jdk-home"));
        assertEquals("persisted SBK_JAVA_HOME configuration",
                SbkUtils.getJavaSelectionSource(Map.of("SBK_JAVA_SOURCE", "SBK_JAVA_HOME_PERSISTED"), "jdk-home"));
        assertEquals("SBK-GEM selected remote JDK",
                SbkUtils.getJavaSelectionSource(Map.of("SBK_JAVA_SOURCE", "SBK_GEM_REMOTE_JDK"), "jdk-home"));
    }

    @Test
    public void testJavaSelectionSourceFallsBackToMatchingEnvironmentHome() {
        assertEquals("SBK_JAVA_HOME environment variable",
                SbkUtils.getJavaSelectionSource(Map.of("SBK_JAVA_HOME", "jdk-home"), "jdk-home"));
        assertEquals("JAVA_HOME environment variable",
                SbkUtils.getJavaSelectionSource(Map.of("JAVA_HOME", "jdk-home"), "jdk-home"));
        assertEquals("running JVM (system PATH or direct Java invocation)",
                SbkUtils.getJavaSelectionSource(Map.of(), "jdk-home"));
    }
}
