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
import org.openjdk.jcstress.infra.results.L_Result;

/**
 * Verifies loss-free MPSC linking and FIFO order within each producer.
 *
 * <p>Two producers each publish two nodes. Their operations may interleave in
 * six valid ways, but each producer's first node must precede its second node.
 * The single arbiter acts as the queue's sole consumer.</p>
 */
@JCStressTest
@State
@Outcome(id = "1-2-3-4", expect = Expect.ACCEPTABLE,
        desc = "Producer one completed before producer two.")
@Outcome(id = "1-3-2-4", expect = Expect.ACCEPTABLE,
        desc = "The producer sequences interleaved.")
@Outcome(id = "1-3-4-2", expect = Expect.ACCEPTABLE,
        desc = "Producer two completed between producer-one nodes.")
@Outcome(id = "3-1-2-4", expect = Expect.ACCEPTABLE,
        desc = "Producer one completed between producer-two nodes.")
@Outcome(id = "3-1-4-2", expect = Expect.ACCEPTABLE,
        desc = "The producer sequences interleaved.")
@Outcome(id = "3-4-1-2", expect = Expect.ACCEPTABLE,
        desc = "Producer two completed before producer one.")
@Outcome(expect = Expect.FORBIDDEN,
        desc = "A node was lost, duplicated, or reordered within a producer.")
public class TimeStampMpscQueueProducerOrderStress {
    private final TimeStampMpscQueue queue = new TimeStampMpscQueue();

    /**
     * Adds producer one's ordered pair.
     */
    @Actor
    public void producerOne() {
        queue.add(node(1));
        queue.add(node(2));
    }

    /**
     * Adds producer two's ordered pair.
     */
    @Actor
    public void producerTwo() {
        queue.add(node(3));
        queue.add(node(4));
    }

    /**
     * Drains all four nodes after the producers complete.
     *
     * @param result JCStress result slot
     */
    @Arbiter
    public void arbiter(L_Result result) {
        final StringBuilder order = new StringBuilder();
        for (int index = 0; index < 4; index++) {
            final TimeStampNode node = queue.poll();
            if (index != 0) {
                order.append('-');
            }
            order.append(node == null ? "missing" : node.records);
        }
        result.r1 = order.toString();
    }

    private static TimeStampNode node(int value) {
        return new TimeStampNode(value, value, value, value);
    }
}
