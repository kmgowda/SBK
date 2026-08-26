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

import java.io.IOException;
import java.nio.file.Path;

/** Selects complete or minimal Java and SBK artifacts without changing shared deployment orchestration. */
interface RuntimeCopyPolicy {

    /**
     * Select the immutable policy for one benchmark invocation.
     *
     * @param fullCopy whether complete artifacts are required
     * @return full-copy or minimal-copy policy
     */
    static RuntimeCopyPolicy select(boolean fullCopy) {
        return fullCopy ? FullRuntimeCopyPolicy.INSTANCE : MinimalRuntimeCopyPolicy.INSTANCE;
    }

    /**
     * Human-readable Java artifact name used by shared lifecycle logging.
     *
     * @return Java deployment name
     */
    String javaDeploymentName();

    /**
     * Create or reuse the Java artifact selected by this policy.
     *
     * @param source Java runtime source and cache paths
     * @return managed Java artifact
     * @throws IOException when the selected Java artifact cannot be prepared
     */
    ManagedJavaRuntime createJavaRuntime(JavaRuntimeSource source) throws IOException;

    /**
     * Create or reuse the SBK artifact selected by this policy.
     *
     * @param source SBK distribution, driver, platform, and cache inputs
     * @return immutable SBK runtime bundle
     * @throws IOException when the selected SBK artifact cannot be prepared
     */
    SbkRuntimeBundle createSbkRuntime(SbkRuntimeSource source) throws IOException;

    /** Inputs shared by the full and minimal Java artifact policies. */
    record JavaRuntimeSource(Path javaDirectory, int javaVersion, Path cacheDirectory, Path sbkDirectory) {
    }

    /** Inputs shared by the full and minimal SBK artifact policies. */
    record SbkRuntimeSource(Path sbkDirectory, String sbkCommand, String sbkVersion, int javaVersion,
                            DeploymentPlatform platform, Path cacheDirectory, String driverClass) {
    }
}
