/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.test;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import io.perl.api.impl.ConcurrentLinkedQueueArray;

/**
 * Test class for ConcurrentLinkedQueueArray.
 * This class tests the basic functionality of adding, polling, and clearing elements
 * in a concurrent linked queue implemented as an array.
 */
public class ConcurrentLinkedQueueArrayTest {

    private ConcurrentLinkedQueueArray<String> queueArray;

    @Before
    public void setUp() {
        queueArray = new ConcurrentLinkedQueueArray<>(3);
    }

    @Test
    public void testAddAndPoll() {
        assertTrue(queueArray.add(0, "A"));
        assertTrue(queueArray.add(1, "B"));
        assertTrue(queueArray.add(2, "C"));

        assertEquals("A", queueArray.poll(0));
        assertEquals("B", queueArray.poll(1));
        assertEquals("C", queueArray.poll(2));
        assertNull(queueArray.poll(0));
    }

    @Test
    public void testClearIndex() {
        queueArray.add(0, "X");
        queueArray.add(0, "Y");
        queueArray.clear(0);
        assertNull(queueArray.poll(0));
    }

    @Test
    public void testClearAll() {
        queueArray.add(0, "A");
        queueArray.add(1, "B");
        queueArray.add(2, "C");
        queueArray.clear();
        assertNull(queueArray.poll(0));
        assertNull(queueArray.poll(1));
        assertNull(queueArray.poll(2));
    }

    @Test
    public void testTypeSafety() {
        ConcurrentLinkedQueueArray<Integer> intArray = new ConcurrentLinkedQueueArray<>(2);
        assertTrue(intArray.add(1, 42));
        assertEquals(Integer.valueOf(42), intArray.poll(1));
    }
}