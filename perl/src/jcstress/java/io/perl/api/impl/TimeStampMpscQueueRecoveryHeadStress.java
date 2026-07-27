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
 * Exercises acquire/release publication of the recovery-head fallback.
 *
 * <p>A batch size of one is an intentionally aggressive test configuration.
 * The queue starts with one node while the producer tail still points to the
 * sentinel. The producer races to append a second node as the consumer
 * release-publishes the first node as recovery head and self-links the
 * sentinel. If the consumer wins, the producer must acquire-read the recovery
 * head; if the producer wins, normal linking applies. Both orders must retain
 * the same FIFO result without any external synchronization edge.</p>
 */
@JCStressTest
@State
@Outcome(id = "1, 2", expect = Expect.ACCEPTABLE,
        desc = "The consumer retired the sentinel and the producer recovered.")
@Outcome(expect = Expect.FORBIDDEN,
        desc = "Recovery-head publication lost or stranded a timestamp.")
public class TimeStampMpscQueueRecoveryHeadStress {
    private final TimeStampMpscQueue queue = new TimeStampMpscQueue(1);

    /**
     * Seeds one node while leaving the producer tail at the sentinel.
     */
    public TimeStampMpscQueueRecoveryHeadStress() {
        queue.add(node(1));
    }

    /**
     * Races to append a second node through the normal or recovery path.
     */
    @Actor
    public void producer() {
        queue.add(node(2));
    }

    /**
     * Retires the sentinel while the producer may still reference it.
     *
     * @param result JCStress result slots
     */
    @Actor
    public void consumer(II_Result result) {
        result.r1 = value(queue.poll());
    }

    /**
     * Confirms reachability through the acquire-read recovery head.
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
