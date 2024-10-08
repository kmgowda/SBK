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
final public class AtomicQueue<T> implements Queue<T> {

    static final private class Node<T> {
        public  T item;
        public AtomicReference<Node<T>> next;
        Node(T item) {
            this.item = item;
            this.next = new AtomicReference<>(null);
        }
    }

    final private Node<T> firstNode;
    final private  AtomicReference<Node<T>> head;
    final private AtomicReference<Node<T>> tail;


    public AtomicQueue() {
        this.firstNode = new Node<>(null);
        this.head = new AtomicReference<>(firstNode);
        this.tail = new AtomicReference<>(firstNode);
    }

    public T pollOnce() {
        final Node<T> curHead = head.getAndSet(null);
        if (curHead == null) {
            return null;
        }
        final Node<T> nxt = curHead.next.getAndSet(null);
        if (nxt == null) {
            head.set(curHead);
            return null;
        }
        head.set(nxt);
        return nxt.item;
    }

    @Override
    public T poll() {
        Node<T> curHead = head.get();
        Node<T> nxt = curHead.next.get();

        while ( nxt != null && !head.compareAndSet(curHead, nxt) ) {
            curHead = head.get();
            nxt = curHead.next.get();
        }

        if (nxt == null) {
            return null;
        }
        curHead.next.set(null);

        return nxt.item;
    }


    @Override
    public boolean add(T data) {
        final Node<T> node = new Node<>(data);
        final Node<T> tailnode = tail.getAndSet(node);
        tailnode.next.set(node);
        return true;
    }

    @Override
    public void clear() {
        firstNode.next.set(null);
        Node<T> first = head.getAndSet(firstNode);
        tail.set(first);
        /*
           The below code helps JVM garbage collector to recycle;
           without the below code, out of memory issues are observed
        */
        while ( first != null ) {
            first = first.next.getAndSet(null);
        }
    }


}
