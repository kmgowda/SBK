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

import io.perl.api.impl.TimeStampMpscQueue;

/**
 * Intrusive queue node that is also the PerL timestamp payload.
 *
 * <p>The standard {@link java.util.concurrent.ConcurrentLinkedQueue} stores a
 * {@link TimeStamp} inside a separate private queue node. PerL creates both
 * objects for every measurement. This class combines those two roles: the four
 * immutable timestamp fields are inherited from {@code TimeStamp}, while
 * {@link #next} is the only queue-specific field. Consequently, the optimized
 * MPSC path allocates exactly one object per measurement.</p>
 *
 * <p>A node is single-use. It must be published to at most one
 * {@link TimeStampMpscQueue} and must never be enqueued again after removal.
 * Keeping this constraint internal to PerL avoids reset operations and the ABA
 * hazards associated with recycling intrusive linked nodes.</p>
 */
public final class TimeStampNode extends TimeStamp {
    /*
     * Accessed with VarHandle acquire/release operations by
     * TimeStampMpscQueue. It is intentionally package-private so the linkage
     * is unavailable to PerL users.
     */
    private TimeStampNode next;

    /**
     * Creates a timestamp queue node.
     *
     * @param startTime event start time
     * @param endTime event end time
     * @param records number of records represented by the event
     * @param bytes number of bytes represented by the event
     */
    public TimeStampNode(long startTime, long endTime, int records, int bytes) {
        super(startTime, endTime, records, bytes);
    }

    /**
     * Creates an end-of-stream marker node.
     *
     * @param endTime end time value
     */
    public TimeStampNode(long endTime) {
        super(endTime);
    }
}
