/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.api.impl;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * Verifies publication of every immutable timestamp field to the consumer.
 *
 * <p>The consumer may run before or after the producer. The arbiter runs after
 * both actors and therefore must observe the node if the actor did not. A
 * value of {@code 1} means all fields were visible, {@code 0} means no node
 * was present, and {@code -1} means a partially published node was observed.</p>
 */
@JCStressTest
@State
@Outcome(id = "1, 0", expect = Expect.ACCEPTABLE,
        desc = "The consumer received a completely published timestamp.")
@Outcome(id = "0, 1", expect = Expect.ACCEPTABLE,
        desc = "The consumer ran early and the arbiter received the timestamp.")
@Outcome(expect = Expect.FORBIDDEN,
        desc = "The timestamp was lost, duplicated, or only partly visible.")
public class TimeStampMpscQueuePublicationStress {
    private static final long START_TIME = 11_111_111_111L;
    private static final long END_TIME = 22_222_222_222L;
    private static final int RECORDS = 33_333;
    private static final int BYTES = 44_444;

    private final TimeStampMpscQueue queue = new TimeStampMpscQueue();

    /**
     * Publishes one fully initialized intrusive node.
     */
    @Actor
    public void producer() {
        queue.add(new TimeStampNode(
                START_TIME, END_TIME, RECORDS, BYTES));
    }

    /**
     * Races with the producer and validates any node it receives.
     *
     * @param result JCStress result slots
     */
    @Actor
    public void consumer(II_Result result) {
        result.r1 = validate(queue.poll());
    }

    /**
     * Looks for the node after both actors finish.
     *
     * @param result JCStress result slots
     */
    @Arbiter
    public void arbiter(II_Result result) {
        result.r2 = validate(queue.poll());
    }

    private static int validate(TimeStampNode node) {
        if (node == null) {
            return 0;
        }
        return node.startTime == START_TIME
                && node.endTime == END_TIME
                && node.records == RECORDS
                && node.bytes == BYTES ? 1 : -1;
    }
}
