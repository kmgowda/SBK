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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final public class SbkUtils {

    @Contract("null, _ -> new")
    public static @NotNull String[] removeOptionArgsAndValues(String[] args, String[] opts) {
        if (args == null) {
            return new String[0];
        }
        if (args.length < 2) {
            return args;
        }
        final List<String> optsList = Arrays.asList(opts);
        final List<String> ret = new ArrayList<>(args.length);
        int i = 0;
        while (i < args.length) {
            if (optsList.contains(args[i])) {
                i += 1;
                optsList.remove(args[i]);
            } else {
                ret.add(args[i]);
            }
            i += 1;
        }
        return ret.toArray(new String[0]);
    }

    public static String getArgValue(String[] args, String argName) {
        if (args == null || args.length < 2) {
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

    public static String getClassName(String[] args) {
        return getArgValue(args, Config.CLASS_OPTION_ARG);
    }

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

    public static boolean hasHelp(String[] args) {
            return hasArg(args, Config.HELP_OPTION_ARG);
    }

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

    public static Map<String, String> argsToMap(String[] args, boolean removeArgPrefix) {
        final Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
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

    public static String[] mergeArgs(String[] s1, String[] s2) {
        final Map<String, String> kv1 = argsToMap(s1, false);
        final Map<String, String> kv2 = argsToMap(s2, false);
        kv1.putAll(kv2);
        return mapToArgs(kv1, false);
    }

}

