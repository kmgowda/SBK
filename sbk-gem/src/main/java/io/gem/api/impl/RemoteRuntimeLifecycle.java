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

/** Defines managed-runtime names and the unavoidable remote benchmark launch command. */
final class RemoteRuntimeLifecycle {
    private RemoteRuntimeLifecycle() {
    }

    /**
     * Identify controller-side entries created by managed runtime deployment.
     *
     * <p>These names are reserved at the deployment-parent root and must never
     * become input to a runtime bundle when localhost uses the SBK distribution
     * itself as that parent.</p>
     *
     * @param name top-level entry name
     * @return true for managed runtime directories, transfer artifacts, leases, markers, and locks
     */
    static boolean isManagedArtifact(String name) {
        return RemoteRuntimeFiles.isManagedArtifact(name);
    }

    /**
     * Return the lease file reserved for one remote benchmark command.
     *
     * @param parentDirectory managed-runtime parent directory
     * @param deploymentName immutable runtime directory name
     * @param leaseId unique benchmark-command lease identifier
     * @return absolute remote lease path
     */
    static String leasePath(String parentDirectory, String deploymentName, String leaseId) {
        return RemoteRuntimeFiles.leasePath(parentDirectory, deploymentName, leaseId);
    }

}
