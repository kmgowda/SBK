/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbm.api;

import io.sbp.grpc.MessageLatenciesRecord;

/**
 * Interface RamRegistry.
 */
public interface SbmRegistry {
     final static long BASE_CLIENT_ID_VALUE = 1;

    /**
     * this method returns id.
     *
     * @return returns id
     */
    long getID();

    /**
     * Queue the records.
     *
     * @param record LatenciesRecord
     */
    void enQueue(MessageLatenciesRecord record);

}
