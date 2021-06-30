/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api;

import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;

import javax.annotation.concurrent.NotThreadSafe;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@NotThreadSafe
final public class StoragePackage {
    final private static int MAX_PRINT_WIDTH = 80;
    final private String packageName;
    final private String[] simpleNames;
    final private String[] names;
    final private StringCompareIgnoreCase stringComparator;

    public StoragePackage(String packageName) {
        final Reflections reflections = new Reflections(packageName);
        final Set<Class<? extends Storage>> subTypes = reflections.getSubTypesOf(Storage.class);
        final int size = subTypes.size();
        this.packageName = packageName;
        this.stringComparator = new StringCompareIgnoreCase();
        this.simpleNames = new String[size];
        this.names = new String[size];
        if (size > 0) {
            final AtomicInteger index = new AtomicInteger(0);
            final Map<String, String> classMap = new HashMap<>();
            subTypes.forEach(x -> classMap.put(x.getSimpleName(), x.getName()));
            classMap.keySet().stream().sorted().forEach(x -> {
                final int i = index.get();
                simpleNames[i] = StringUtils.capitalize(x);
                names[i] = classMap.get(x);
                index.incrementAndGet();
            });
        }
    }


    private static class StringCompareIgnoreCase implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            return o1.compareToIgnoreCase(o2);
        }
    }


    public boolean isEmpty() {
        return  simpleNames.length == 0;
    }

    public String[] getDrivers() {
        return simpleNames;
    }

    public Storage<?> getStorage(String storageName) throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        final int i = Arrays.binarySearch(simpleNames, storageName, stringComparator);
        if (i < 0) {
            throw new ClassNotFoundException();
        }
        return getStorageInstance(names[i]);
    }

    public void printDrivers() {
        final String printStr = "Available Drivers in package '"+ packageName+"': "+simpleNames.length;
        final StringBuilder builder = new StringBuilder(printStr);
        builder.append(" [");
        int length = printStr.length() + 2;
        for (int i = 0; i < simpleNames.length; i++) {
            builder.append(simpleNames[i]);
            length += simpleNames[i].length();
            if (i+1 < simpleNames.length) {
                builder.append(", ");
                length += 2;
            }
            if (length > MAX_PRINT_WIDTH && i+1 < simpleNames.length) {
                builder.append("\n");
                length = 0;
            }
        }
        builder.append("]");
        System.out.println(builder);
    }


    public static Storage<?> getStorageInstance(String storageFullPath) throws ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return (Storage<?>) Class.forName(storageFullPath).getConstructor().newInstance();
    }

}
