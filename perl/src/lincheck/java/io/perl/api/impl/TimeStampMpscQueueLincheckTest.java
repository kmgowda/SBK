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

import org.jetbrains.lincheck.datastructures.IntGen;
import org.jetbrains.lincheck.datastructures.ModelCheckingOptions;
import org.jetbrains.lincheck.datastructures.Operation;
import org.jetbrains.lincheck.datastructures.Param;
import org.jetbrains.lincheck.datastructures.StressOptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Lincheck linearizability tests for {@link TimeStampMpscQueue}.
 *
 * <p>Lincheck normally permits every operation to run on every worker. The
 * queue contract permits multiple concurrent {@link #offer(int)} calls but
 * only one consumer, so {@link #poll()} belongs to a non-parallel operation
 * group. The sequential specification is a conventional FIFO queue.</p>
 */
@Param(name = "value", gen = IntGen.class, conf = "1:4")
public class TimeStampMpscQueueLincheckTest {
    private static final String CONSUMER_GROUP = "single-consumer";
    private final TimeStampMpscQueue queue = new TimeStampMpscQueue();

    /**
     * Enqueues a timestamp whose start time carries the model value.
     *
     * @param value generated timestamp value
     * @return {@code true}, matching the unbounded queue contract
     */
    @Operation(params = "value")
    public boolean offer(int value) {
        return queue.add(new TimeStampNode(value, value + 1L, value, value));
    }

    /**
     * Removes the oldest timestamp through the single-consumer operation
     * group.
     *
     * @return the model value, or {@code null} when the queue is empty
     */
    @Operation(nonParallelGroup = CONSUMER_GROUP)
    public Long poll() {
        final TimeStampNode node = queue.poll();
        return node == null ? null : node.startTime;
    }

    /**
     * Explores interleavings and validates them against the FIFO model.
     */
    @Test
    public void modelCheckingPreservesMpscFifoSemantics() {
        new ModelCheckingOptions()
                .iterations(25)
                .invocationsPerIteration(1_000)
                .threads(3)
                .actorsPerThread(3)
                .sequentialSpecification(SequentialQueue.class)
                .check(TimeStampMpscQueueLincheckTest.class);
    }

    /**
     * Exercises the same contract under scheduler and hardware timing.
     */
    @Test
    public void stressPreservesMpscFifoSemantics() {
        new StressOptions()
                .iterations(50)
                .invocationsPerIteration(5_000)
                .threads(3)
                .actorsPerThread(3)
                .sequentialSpecification(SequentialQueue.class)
                .check(TimeStampMpscQueueLincheckTest.class);
    }

    /**
     * Sequential FIFO model used as the linearizability oracle.
     */
    public static final class SequentialQueue {
        private final Queue<Long> values = new ArrayDeque<>();

        /**
         * Adds a value to the model.
         *
         * @param value generated timestamp value
         * @return {@code true}
         */
        public boolean offer(int value) {
            return values.add((long) value);
        }

        /**
         * Removes the oldest model value.
         *
         * @return oldest value, or {@code null} when empty
         */
        public Long poll() {
            return values.poll();
        }
    }
}
