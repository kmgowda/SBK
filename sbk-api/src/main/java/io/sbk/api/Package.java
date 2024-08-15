/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

import io.sbk.system.Printer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Package<T> {
    final private static int MAX_PRINT_WIDTH = 80;
    final private String packageName;
    final private String[] simpleNames;
    final private String[] names;
    final private StringCompareIgnoreCase stringComparator;

    public Package(String packageName) {
        final Map<String, Class<? extends T>> classMap = new HashMap<>();
        getClasses(packageName).forEach(x -> {
            //Exclude the Abstract classes & interfaces
            int mod = x.getModifiers();
            if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod)) {
                classMap.put(x.getSimpleName(), x);
            }
        });
        final int size = classMap.size();
        this.packageName = packageName;
        this.stringComparator = new StringCompareIgnoreCase();
        this.simpleNames = new String[size];
        this.names = new String[size];
        if (size > 0) {
            final AtomicInteger index = new AtomicInteger(0);
            classMap.keySet().stream().sorted(String::compareToIgnoreCase).forEach(x -> {
                final int i = index.get();
                simpleNames[i] = StringUtils.capitalize(x);
                names[i] = classMap.get(x).getName();
                index.incrementAndGet();
            });
        }
    }

    /**
     * Get the set of Available Storage classes.
     *
     * @param packageName Name of the package.
     * @return Set of classes extends Storage class
     */
    public abstract Set<Class<? extends T>> getClasses(String packageName);

    private  @NotNull T getClassInstance(String storageFullPath) throws ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return (T) Class.forName(storageFullPath).getConstructor().newInstance();
    }

    protected boolean isEmpty() {
        return simpleNames.length == 0;
    }

    public String[] getClassNames() {
        return simpleNames.clone();
    }

    public @NotNull T getClass(String classNAme) throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        final int i = Arrays.binarySearch(simpleNames, classNAme, stringComparator);
        if (i < 0) {
            throw new ClassNotFoundException("class '" + classNAme + "' not found in package: " + packageName);
        }
        return getClassInstance(names[i]);
    }

    public void printClasses(String header) {
        final String printStr = header + " Classes in package '" + packageName + "': " + simpleNames.length;
        final StringBuilder builder = new StringBuilder(printStr);
        builder.append(" [");
        int length = printStr.length() + 30;
        for (int i = 0; i < simpleNames.length; i++) {
            builder.append(simpleNames[i]);
            length += simpleNames[i].length();
            if (i + 1 < simpleNames.length) {
                builder.append(", ");
                length += 2;
            }
            if (length > MAX_PRINT_WIDTH && i + 1 < simpleNames.length) {
                builder.append("\n");
                length = 0;
            }
        }
        builder.append("]");
        Printer.log.info(String.valueOf(builder));
    }

    /**
     *  class StringCompareIgnoreCase.
     */
    private static class StringCompareIgnoreCase implements Comparator<String>, Serializable {

        @Override
        @Contract(pure = true)
        public int compare(@NotNull String o1, String o2) {
            return o1.compareToIgnoreCase(o2);
        }
    }

}
