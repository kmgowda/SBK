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
import org.openjdk.jcstress.infra.results.III_Result;

/**
 * Verifies that lifecycle emptiness observation cannot miss a published node
 * while the single consumer advances its non-volatile head.
 *
 * <p>Both nodes are published before the consumer and observer proceed. The
 * consumer removes exactly the first node. Regardless of whether the observer
 * reads the old or new consumer head, it must see a linked successor and report
 * non-empty. The arbiter must then receive the second node.</p>
 */
@JCStressTest
@State
@Outcome(id = "1, 0, 2", expect = Expect.ACCEPTABLE,
        desc = "The consumer advanced and lifecycle observation retained the published tail.")
@Outcome(expect = Expect.FORBIDDEN,
        desc = "Lifecycle observation missed, reordered, or lost a published node.")
public class TimeStampMpscQueueLifecycleEmptyStress {
    private final TimeStampMpscQueue queue = new TimeStampMpscQueue();
    private volatile boolean published;

    /**
     * Publishes two nodes before releasing the consumer and lifecycle observer.
     */
    @Actor
    public void producer() {
        queue.add(node(1));
        queue.add(node(2));
        published = true;
    }

    /**
     * Advances the consumer-owned head once.
     *
     * @param result JCStress result slots
     */
    @Actor
    public void consumer(III_Result result) {
        awaitPublication();
        result.r1 = value(queue.poll());
    }

    /**
     * Observes emptiness concurrently with the consumer head advance.
     *
     * @param result JCStress result slots
     */
    @Actor
    public void lifecycleObserver(III_Result result) {
        awaitPublication();
        result.r2 = queue.isEmpty() ? 1 : 0;
    }

    /**
     * Confirms that the unconsumed published node remains reachable.
     *
     * @param result JCStress result slots
     */
    @Arbiter
    public void arbiter(III_Result result) {
        result.r3 = value(queue.poll());
    }

    private void awaitPublication() {
        while (!published) {
            Thread.onSpinWait();
        }
    }

    private static TimeStampNode node(int value) {
        return new TimeStampNode(value, value, value, value);
    }

    private static int value(TimeStampNode node) {
        return node == null ? -1 : node.records;
    }
}
