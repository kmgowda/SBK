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

import io.perl.api.Queue;
import java.util.concurrent.atomic.AtomicReference;

/*
 * Concurrent Queue Implementation using Atomic References.
 * DON'T USE THIS CLASS.
 * Use Java native 'ConcurrentLinkedQueue', because the ConcurrentLinkedQueue does better Garbage collection.
 */
final public class CQueue<T> implements Queue<T> {
    final private Node<T> firstNode;
    final private  AtomicReference<Node<T>> head;
    final private  AtomicReference<Node<T>> tail;

    public CQueue() {
        this.firstNode = new Node<>(null);
        this.head = new AtomicReference<>(firstNode);
        this.tail = new AtomicReference<>(firstNode);
    }

    @Override
    public T poll() {
        final Node<T> first = head.get();
        final Node<T> cur = first.next.getAndSet(null);
        if (cur == null) {
            return null;
        }
        head.set(cur);
        return cur.item;
    }

    @Override
    public boolean add(T data) {
        final Node<T> node = new Node<>(data);
        final Node<T> cur = tail.getAndSet(node);
        cur.next.set(node);
        return true;
    }

    @Override
    public void clear() {
        Node<T> first = head.getAndSet(firstNode);
        tail.set(firstNode);
        /*
           The below code helps JVM garbage collector to recycle;
           without the below code, out of memory issues are observed
        */
        while ( first != null ) {
            first = first.next.getAndSet(null);
        }
    }

    static final private class Node<T> {
        final public T item;
        final public AtomicReference<Node<T>> next;
        Node(T item) {
            this.item = item;
            this.next = new AtomicReference<>(null);
        }
    }
}
