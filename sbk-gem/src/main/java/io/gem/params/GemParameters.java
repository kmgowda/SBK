/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.params;

import io.gem.api.ConnectionConfig;
import io.sbk.params.Parameters;

import java.math.BigDecimal;

/**
 * Interface GemParameters.
 */
public sealed interface GemParameters extends Parameters permits GemParameterOptions {

    /**
     * to get ssh connections.
     *
     * @return SshConnection.
     */
    ConnectionConfig[] getConnections();

    /**
     * to get Sbk directory.
     *
     * @return Sbk directory.
     */
    String getSbkDir();

    /**
     * to get local host.
     *
     * @return the local host.
     */
    String getLocalHost();

    /**
     * to get SBM port number.
     *
     * @return SBM port number.
     */
    int getSbmPort();


    /**
     * to get SBM idle milliseconds sleep.
     *
     * @return SBM idle milliseconds sleep.
     */
    int getSbmIdleSleepMilliSeconds();

    /**
     * Check whether the record value is an aggregate across all remote SBK clients.
     *
     * @return true when {@code -totalrecords} was supplied
     */
    boolean isTotalRecordsOption();

    /**
     * Check whether throughput is an aggregate across all remote SBK clients.
     *
     * @return true when {@code -totalthroughput} was supplied
     */
    boolean isTotalThroughputOption();

    /**
     * Get the aggregate throughput requested for all remote SBK clients.
     *
     * @return aggregate throughput in MB/s
     */
    BigDecimal getTotalThroughput();


    /**
     * Get the required remote Java major version.
     *
     * @return Java major version
     */
    int getJavaVersion();

    /**
     * Get the optional remote Java home.
     *
     * @return remote Java home, or an empty value when automatic discovery is used
     */
    String getJavaDir();

    /**
     * Check whether a mismatched SBK installation may be deleted before replacement.
     *
     * @return true when pre-copy deletion is enabled
     */
    boolean isDelete();

    /**
     * Check whether every inactive non-current SBK-GEM-managed runtime and local cached bundle should be removed.
     *
     * @return true when managed runtime cleanup is enabled
     */
    boolean isRuntimeCleanup();
}
