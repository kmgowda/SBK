/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbm.api;

import io.sbk.api.Package;
import io.sbm.logger.RamLogger;
import org.reflections.Reflections;

import java.util.Set;

/**
 * Reflection-backed package scanner for {@link RamLogger} implementations.
 *
 * <p>Used to discover available SBM logger classes under a given package so the
 * application can present choices and instantiate the selected logger.
 */
public class RamLoggerPackage extends Package<RamLogger> {

    public RamLoggerPackage(String packageName) {
        super(packageName);
    }

    @Override
    public Set<Class<? extends RamLogger>> getClasses(String packageName) {
        final Reflections reflections = new Reflections(packageName);
        return reflections.getSubTypesOf(RamLogger.class);
    }
}