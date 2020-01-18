/**
 * Copyright (c) 2020 KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.dsb.api;

/**
 * Interface for recoding results.
 */
public interface ResultLogger {

    public void print(String action, long records, double recsPerSec, double mbPerSec, double avglatency, double maxlatency);

    public void printLatencies(String action, int one, int two, int three, int four, int five, int six);

    public void printDiscardedLatencies(String action, int discard);
}
