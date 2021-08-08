/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.perl;

/**
 * Interface for recording latencies.
 */
public interface TotalPeriodicWindow extends PeriodicWindow {

    /**
     * Start the Total Window.
     *
     * @param startTime starting time.
     */
    void start(long startTime);


    /**
     * Stop the Total Window.
     *
     * @param endTime current time.
     */
    void stop(long endTime);
}
