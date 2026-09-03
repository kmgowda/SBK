/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.api;

/**
 * Channel is a combined API that exposes both producer and consumer behavior
 * for the PerL internals. It extends {@link GetPerlChannel} to allow obtaining
 * per-thread channels and adds methods used by internal recorder threads to
 * receive timestamped events.
 *
 * <p>Semantics:
 * <ul>
 *     <li>{@link #receive(int)} returns the next {@link TimeStamp} immediately, or
 *     {@code null} when no data is available. Waiting policy belongs to the
 *     recorder loop rather than the queue abstraction.</li>
 *     <li>{@link #sendEndTime(long)} is used to notify the channel consumers
 *     that the benchmark is ending; implementations should use this to unblock
 *     waiting consumers where appropriate.</li>
 *     <li>{@link #clear()} should remove any queued data and reset internal
 *     state so the channel can be reused.</li>
 * </ul>
 *
 * Note: caller-side thread usage should follow the guideline that each
 * recording thread has its own {@link PerlChannel} instance obtained via
 * {@link GetPerlChannel#getPerlChannel()}.
 */
public non-sealed interface Channel extends GetPerlChannel {

    /**
     * Poll for the next benchmarking timestamp.
     *
     * @param timeout ignored; retained for source compatibility
     * @return the next timestamp, or {@code null} when no timestamp is available
     */
    TimeStamp receive(int timeout);

    /**
     * Send an end time indication to consumers. This is typically invoked by
     * the producer to signal shutdown.
     *
     * @param endTime End Time.
     */
    void sendEndTime(long endTime);

    /**
     * Clear the channel of any pending data and reset its internal state.
     */
    void clear();
}
