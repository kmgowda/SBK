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
import lombok.Synchronized;

import java.util.LinkedList;

public class SyncQueue<T> implements Queue<T> {

    private final LinkedList<T> list;

    public SyncQueue() {
        list = new LinkedList<>();
    }

    @Override
    @Synchronized
    public T poll() {
        return list.poll();
    }

    @Override
    @Synchronized
    public boolean add(T data) {
        return list.add(data);
    }

    @Override
    @Synchronized
    public void clear() {
            list.clear();
    }
}
