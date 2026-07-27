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

import io.perl.api.TimeStampNode;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * Exercises stale-producer recovery after a two-node retirement batch.
 *
 * <p>The stale producer pauses after reading the sentinel as its tail hint.
 * The coordinator appends and consumes a second node, causing the batch of
 * two predecessors to be self-linked, then releases the producer. The
 * producer must detect its self-linked sentinel, recover through the newer
 * tail, and publish its node without loss.</p>
 */
@JCStressTest
@State
@Outcome(id = "12, 3", expect = Expect.ACCEPTABLE,
        desc = "The retired batch was consumed and the stale producer recovered.")
@Outcome(expect = Expect.FORBIDDEN,
        desc = "Retirement lost, duplicated, reordered, or stranded a node.")
public class TimeStampMpscQueueRetirementRecoveryStress {
    private final TimeStampMpscQueue queue = new TimeStampMpscQueue(2);
    private volatile int phase;

    /**
     * Seeds the queue so its tail may lag at the sentinel.
     */
    public TimeStampMpscQueueRetirementRecoveryStress() {
        queue.add(node(1));
    }

    /**
     * Pauses with a tail reference that the consumer will retire.
     */
    @Actor
    public void staleProducer() {
        queue.add(node(3), () -> {
            phase = 1;
            while (phase != 2) {
                Thread.onSpinWait();
            }
        });
    }

    /**
     * Completes a retirement batch while the stale producer is paused.
     *
     * @param result JCStress result slots
     */
    @Actor
    public void coordinator(II_Result result) {
        while (phase != 1) {
            Thread.onSpinWait();
        }
        queue.add(node(2));
        final TimeStampNode first = queue.poll();
        final TimeStampNode second = queue.poll();
        result.r1 = value(first) * 10 + value(second);
        phase = 2;
    }

    /**
     * Confirms that the recovered producer's node remains reachable.
     *
     * @param result JCStress result slots
     */
    @Arbiter
    public void arbiter(II_Result result) {
        result.r2 = value(queue.poll());
    }

    private static TimeStampNode node(int value) {
        return new TimeStampNode(value, value, value, value);
    }

    private static int value(TimeStampNode node) {
        return node == null ? -1 : node.records;
    }
}
