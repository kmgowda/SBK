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
    final private  Node<T> nullNode;
    final private  AtomicReference<Node<T>> head;
    final private  AtomicReference<Node<T>> tail;
    private Node<T> prevHead;

    public CQueue() {
        this.nullNode = new Node<>(null);
        this.head = new AtomicReference<>(this.nullNode);
        this.tail = new AtomicReference<>(null);
        this.prevHead = null;
    }

    @Override
    public T poll() {
        Node<T> first = head.getAndSet(nullNode);
        if (first == nullNode) {
            return null;
        }
        if (first == null && prevHead != null) {
            first = prevHead.next;
        }
        if (first == null) {
            head.set(null);
            return null;
        }
        final Node<T> prev = prevHead;
        prevHead = first;
        head.set(first.next);
        if (prev != null && prev != first) {
            prev.next = null;
        }
        return first.item;
    }

    @Override
    public boolean add(T data) {
        final Node<T> node = new Node<>(data);
        final Node<T> cur = tail.getAndSet(node);
        if (cur == null) {
            head.set(node);
        } else {
            cur.next = node;
        }
        return true;
    }

    @Override
    public void clear() {
        Node<T> prevFirst = prevHead;
        Node<T> first = head.getAndSet(null);
        tail.set(null);
        Node<T> cur;
        while ( prevFirst != null ) {
            cur = prevFirst;
            prevFirst = prevFirst.next;
            cur.next = null;
        }
        while ( first != null ) {
            cur = first;
            first = first.next;
            cur.next = null;
        }
    }

    static final class Node<T> {
        final public T item;
        public volatile Node<T> next;
        Node(T item) {
            this.item = item;
            this.next = null;
        }
    }
}
