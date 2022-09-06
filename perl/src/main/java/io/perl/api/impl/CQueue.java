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
        if (first.next == null) {
            return null;
        }
        head.set(first.next);
        final Node<T> cur = first.next;
        /*
            The below code helps JVM garbage collector to recycle;
            without the below code, out of memory issues are observed
         */
        first.next = null;
        return cur.item;
    }

    @Override
    public boolean add(T data) {
        final Node<T> node = new Node<>(data);
        final Node<T> cur = tail.getAndSet(node);
        cur.next = node;
        return true;
    }

    @Override
    public void clear() {
        Node<T> first = head.getAndSet(firstNode);
        tail.set(firstNode);
        Node<T> cur;
        /*
           The below code helps JVM garbage collector to recycle;
           without the below code, out of memory issues are observed
        */
        while ( first != null ) {
            cur = first;
            first = first.next;
            cur.next = null;
        }
    }

    static final private class Node<T> {
        final public T item;
        public volatile Node<T> next;
        Node(T item) {
            this.item = item;
            this.next = null;
        }
    }
}
