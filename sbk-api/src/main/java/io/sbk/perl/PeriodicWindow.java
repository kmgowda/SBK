/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.perl;

/**
 * Interface for Periodic window.
 */
public interface PeriodicWindow {

    /**
     * Start the Recording window.
     *
     * @param startTime starting time.
     */
    void startWindow(long startTime);

    /**
     * Get the current time duration of this window starting from startWindow.
     *
     * @param currentTime current time.
     * @return elapsed Time in Milliseconds from the startTime.
     */
    long elapsedMilliSecondsWindow(long currentTime);

    /**
     * Stop the Recording window.
     * Results from startWindow to this method are printed /flushed.
     *
     * @param stopTime  stopTime.
     */
    void stopWindow(long stopTime);
}
