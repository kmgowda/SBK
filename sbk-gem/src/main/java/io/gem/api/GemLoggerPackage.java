/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api;

import io.sbk.api.Package;
import io.gem.logger.GemLogger;
import org.reflections.Reflections;

import java.util.Set;

/**
 * Reflection-backed package scanner for {@link GemLogger} implementations.
 *
 * <p>Provides discovery utilities to enumerate and load logger classes under a given
 * package name for SBK-GEM. Used by orchestrators to present available logger choices
 * and to instantiate the selected one.
 */
public final class GemLoggerPackage extends Package<GemLogger> {

    public GemLoggerPackage(String packageName) {
        super(packageName);
    }

    @Override
    public Set<Class<? extends GemLogger>> getClasses(String packageName) {
        final Reflections reflections = new Reflections(packageName);
        return reflections.getSubTypesOf(GemLogger.class);
    }
}
