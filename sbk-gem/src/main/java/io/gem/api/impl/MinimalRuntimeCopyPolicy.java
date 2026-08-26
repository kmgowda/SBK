/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api.impl;

import io.sbk.system.Printer;

import java.io.IOException;

/** Compact-Java and selected-driver SBK runtime copy policy. */
final class MinimalRuntimeCopyPolicy implements RuntimeCopyPolicy {
    static final MinimalRuntimeCopyPolicy INSTANCE = new MinimalRuntimeCopyPolicy();
    private static final String JAVA_DEPLOYMENT_NAME = "compact Java runtime";

    private MinimalRuntimeCopyPolicy() {
    }

    @Override
    public String javaDeploymentName() {
        return JAVA_DEPLOYMENT_NAME;
    }

    @Override
    public ManagedJavaRuntime createJavaRuntime(JavaRuntimeSource source) throws IOException {
        final CompactJavaRuntimeDescriptor descriptor = CompactJavaRuntimeDescriptor.load(
                source.sbkDirectory(), source.javaVersion());
        return ManagedJavaRuntime.createCompact(source.javaDirectory(), source.javaVersion(),
                source.cacheDirectory(), descriptor);
    }

    @Override
    public SbkRuntimeBundle createSbkRuntime(SbkRuntimeSource source) throws IOException {
        final DriverRuntimeManifest driverRuntime = DriverRuntimeManifest.load(source.sbkDirectory(),
                source.driverClass(), source.sbkVersion());
        Printer.log.info("SBK-GEM: Minimal runtime copy is enabled; selected SBK driver '{}'",
                driverRuntime.driverName());
        return SbkRuntimeBundle.create(source.sbkDirectory(), source.sbkCommand(), source.sbkVersion(),
                source.javaVersion(), source.platform(), source.cacheDirectory(), driverRuntime);
    }
}
