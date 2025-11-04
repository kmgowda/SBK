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


import org.reflections.Reflections;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Set;

/**
 * Concrete Package implementation for discovering {@link io.sbk.api.Storage}
 * implementations on the classpath. It uses Reflections to locate subtypes.
 */
@NotThreadSafe
final public class StoragePackage extends Package<Storage<?>> {
    public StoragePackage(String packageName) {
        super(packageName);
    }

    /**
     * Get the set of Available Storage classes.
     *
     * @param packageName Name of the package.
     * @return Set of classes extends Storage class
     */
    @SuppressWarnings("unchecked")
    @Override
    public Set<Class<? extends Storage<?>>> getClasses(String packageName) {
        final Reflections reflections = new Reflections(packageName);
        return (Set<Class<? extends Storage<?>>>) (Set<?>) reflections.getSubTypesOf(Storage.class);
    }

}
