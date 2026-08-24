/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.utils;

import io.sbk.config.Config;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Utility methods for inspecting, filtering, and merging SBK arguments. */
public final class SbkUtils {
    private static final String OS_NAME_PROPERTY = "os.name";
    private static final String OS_VERSION_PROPERTY = "os.version";
    private static final String OS_ARCH_PROPERTY = "os.arch";
    private static final String UNKNOWN_SYSTEM_PROPERTY = "unknown";
    private static final String[] SENSITIVE_OPTIONS = {
        "gempass", "key", "password", "passwd", "secret", "token"
    };

    /**
     * Creates an SBK argument utility.
     */
    public SbkUtils() {
    }

    /**
     * Return a copy of command-line arguments with values belonging to sensitive
     * options replaced by a fixed mask. Both {@code -option value} and
     * {@code --option=value} forms are supported, without modifying the source
     * array.
     *
     * @param args command-line arguments
     * @param sensitiveOptions sensitive option names, with or without leading dashes
     * @return copied arguments with sensitive values redacted
     */
    public static @NotNull String[] redactOptionValues(String[] args, String[] sensitiveOptions) {
        if (args == null) {
            return new String[0];
        }
        final String[] redacted = Arrays.copyOf(args, args.length);
        if (sensitiveOptions == null || sensitiveOptions.length == 0) {
            return redacted;
        }
        final Set<String> sensitiveNames = Stream.of(sensitiveOptions)
                .map(SbkUtils::normalizeOptionName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        int i = 0;
        while (i < redacted.length) {
            final String argument = redacted[i];
            final int equalsIndex = argument.indexOf('=');
            final String option = equalsIndex >= 0 ? argument.substring(0, equalsIndex) : argument;
            if (!option.startsWith(Config.ARG_PREFIX) ||
                    !sensitiveNames.contains(normalizeOptionName(option).toLowerCase(Locale.ROOT))) {
                i++;
                continue;
            }
            if (equalsIndex >= 0) {
                redacted[i] = argument.substring(0, equalsIndex + 1) + "******";
                i++;
            } else if (i + 1 < redacted.length) {
                redacted[i + 1] = "******";
                i += Config.OPTION_PAIR_WIDTH;
            } else {
                i++;
            }
        }
        return redacted;
    }

    /**
     * Return a copy of SBK arguments with values for commonly used credential
     * options replaced by a fixed mask.
     *
     * @param args command-line arguments
     * @return copied arguments with credential values redacted
     */
    public static @NotNull String[] redactSensitiveOptionValues(String[] args) {
        return redactOptionValues(args, SENSITIVE_OPTIONS);
    }

    /**
     * Returns the operating-system name, version, and architecture reported by the JVM.
     *
     * @return operating-system details suitable for startup diagnostics
     */
    public static @NotNull String getOperatingSystemDetails() {
        return System.getProperty(OS_NAME_PROPERTY, UNKNOWN_SYSTEM_PROPERTY) + " "
                + System.getProperty(OS_VERSION_PROPERTY, UNKNOWN_SYSTEM_PROPERTY) + " ("
                + System.getProperty(OS_ARCH_PROPERTY, UNKNOWN_SYSTEM_PROPERTY) + ")";
    }

    /**
     * Removes each named option and its following value once.
     *
     * @param args source arguments
     * @param opts option names to remove
     * @return filtered arguments
     */
    @Contract("null, _ -> new")
    public static @NotNull String[] removeOptionArgsAndValues(String[] args, String[] opts) {
        if (args == null) {
            return new String[0];
        }
        if (args.length < 1) {
            return args;
        }
        final List<String> optsList =  Stream.of(opts).collect(Collectors.toList());
        final List<String> ret = new ArrayList<>(args.length);
        int i = 0;
        while (i < args.length) {
            if (optsList.contains(args[i])) {
                optsList.remove(args[i]);
                i += 1;
            } else {
                ret.add(args[i]);
            }
            i += 1;
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Removes each named option once while retaining other arguments.
     *
     * @param args source arguments
     * @param opts option names to remove
     * @return filtered arguments
     */
    @Contract("null, _ -> new")
    public static @NotNull String[] removeOptionArgs(String[] args, String[] opts) {
        if (args == null) {
            return new String[0];
        }
        if (args.length < 1) {
            return args;
        }
        final List<String> optsList =  Stream.of(opts).collect(Collectors.toList());
        final List<String> ret = new ArrayList<>(args.length);
        int i = 0;
        while (i < args.length) {
            if (optsList.contains(args[i])) {
                optsList.remove(args[i]);
            } else {
                ret.add(args[i]);
            }
            i += 1;
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Finds the value following an option.
     *
     * @param args source arguments
     * @param argName option name
     * @return option value, or an empty string when absent
     */
    public static String getArgValue(String[] args, String argName) {
        if (args == null || args.length < Config.OPTION_PAIR_WIDTH) {
            return "";
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(argName)) {
                if (i + 1 < args.length) {
                    return args[i + 1];
                } else {
                    return "";
                }
            }
        }
        return "";
    }

    /**
     * Returns the selected storage class name.
     *
     * @param args source arguments
     * @return storage class name, or an empty string when absent
     */
    public static String getClassName(String[] args) {
        return getArgValue(args, Config.CLASS_OPTION_ARG);
    }

    /**
     * Returns the selected logger name.
     *
     * @param args source arguments
     * @return logger name, or an empty string when absent
     */
    public static String getLoggerName(String[] args) {
        return getArgValue(args, Config.LOGGER_OPTION_ARG);
    }

    /**
     * Tests whether an argument is present.
     *
     * @param args source arguments
     * @param argName argument to find
     * @return {@code true} when present
     */
    public static boolean hasArg(String[] args, String argName) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (arg.equals(argName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether the help option is present.
     *
     * @param args source arguments
     * @return {@code true} when help was requested
     */
    public static boolean hasHelp(String[] args) {
            return hasArg(args, Config.HELP_OPTION_ARG);
    }

    /**
     * Tests whether either version option is present.
     *
     * @param args source arguments
     * @return {@code true} when version information was requested
     */
    public static boolean hasVersion(String[] args) {
            return hasArg(args, Config.VERSION_OPTION_ARG) || hasArg(args, Config.VERSION_OPTION_ARG_SHORT);
    }

    /**
     * Converts ordered option/value mappings to an argument array.
     *
     * @param map option/value mappings
     * @param addArgPrefix whether to prepend the standard argument prefix
     * @return argument array
     */
    public static String[] mapToArgs(Map<String, String> map, boolean addArgPrefix) {
        final List<String> lt = new ArrayList<>();
        map.forEach((k, v) -> {
            if (addArgPrefix) {
                lt.add(Config.ARG_PREFIX + k.strip());
            } else {
                lt.add(k.strip());
            }
            lt.add(v.replaceAll("\\n+", " ").strip());
        });
        return lt.toArray(new String[0]);
    }

    /**
     * Converts option/value argument pairs to an ordered map.
     *
     * @param args option/value argument pairs
     * @param removeArgPrefix whether to remove the standard argument prefix
     * @return ordered option/value mappings
     */
    public static Map<String, String> argsToMap(String[] args, boolean removeArgPrefix) {
        final Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i += Config.OPTION_PAIR_WIDTH) {
            String name = args[i].strip();
            final String key = name.startsWith(Config.ARG_PREFIX) && removeArgPrefix ? args[i].substring(1) : name;
            String val = "";
            if (i+1 < args.length) {
                val = args[i+1].strip();
            }
            map.put(key, val);
        }
        return map;
    }

    /**
     * Merges two option/value arrays, with the second array taking precedence.
     *
     * @param s1 base arguments
     * @param s2 overriding arguments
     * @return merged argument array without duplicate options
     */
    public static String[] mergeArgs(String[] s1, String[] s2) {
        final Map<String, String> merged = new LinkedHashMap<>();
        mergeArgsInto(merged, s1);
        mergeArgsInto(merged, s2);
        return mapToArgs(merged, true);
    }

    private static void mergeArgsInto(Map<String, String> merged, String[] args) {
        if (args == null) {
            return;
        }
        for (int i = 0; i < args.length; i += Config.OPTION_PAIR_WIDTH) {
            final String option = normalizeOptionName(args[i]);
            final String value = i + 1 < args.length ? args[i + 1].strip() : "";
            merged.put(option, value);
        }
    }

    private static String normalizeOptionName(String option) {
        String normalized = option.strip();
        while (normalized.startsWith(Config.ARG_PREFIX)) {
            normalized = normalized.substring(Config.ARG_PREFIX.length());
        }
        return normalized;
    }

}
