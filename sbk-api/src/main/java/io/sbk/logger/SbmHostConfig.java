/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger;

/**
 * Configuration for the SBK Benchmark Manager (SBM) gRPC endpoint.
 *
 * Used by {@code io.sbk.logger.impl.GrpcLogger} to connect and stream metrics to SBM.
 */
public final class SbmHostConfig {
    /* Hostname or IP address of the SBM server. */
    public String host;
    /* Port of the SBM server. */
    public int port;
    /* Maximum record payload size (in MB) permitted when sending latencies. */
    public int maxRecordSizeMB;
}
