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
import java.net.ConnectException;
import java.util.concurrent.ExecutionException;

/** Remote transport operations required by the deployment coordinator. */
interface DeploymentTransport {
    boolean[] missingTargets(SbkRuntimeBundle bundle, DeploymentPlatform platform)
            throws IOException, InterruptedException, ExecutionException;

    void uploadAndActivate(SbkRuntimeBundle bundle, boolean[] copyTargets, DeploymentPlatform platform)
            throws ConnectException, InterruptedException, ExecutionException, IOException;

    void verify(SbkRuntimeBundle bundle, boolean[] copyTargets, DeploymentPlatform platform)
            throws ConnectException, InterruptedException, ExecutionException, IOException;
}
