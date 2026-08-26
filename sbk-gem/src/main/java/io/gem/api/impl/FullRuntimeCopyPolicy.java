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

/** Complete controller-JDK and complete SBK-distribution copy policy. */
final class FullRuntimeCopyPolicy implements RuntimeCopyPolicy {
    static final FullRuntimeCopyPolicy INSTANCE = new FullRuntimeCopyPolicy();
    private static final String JAVA_DEPLOYMENT_NAME = "full JDK";

    private FullRuntimeCopyPolicy() {
    }

    @Override
    public String javaDeploymentName() {
        return JAVA_DEPLOYMENT_NAME;
    }

    @Override
    public ManagedJavaRuntime createJavaRuntime(JavaRuntimeSource source) throws IOException {
        return ManagedJavaRuntime.create(source.javaDirectory(), source.javaVersion(), source.cacheDirectory());
    }

    @Override
    public SbkRuntimeBundle createSbkRuntime(SbkRuntimeSource source) throws IOException {
        Printer.log.info("SBK-GEM: Complete SBK distribution deployment is enabled");
        return SbkRuntimeBundle.create(source.sbkDirectory(), source.sbkCommand(), source.sbkVersion(),
                source.javaVersion(), source.platform(), source.cacheDirectory());
    }
}
