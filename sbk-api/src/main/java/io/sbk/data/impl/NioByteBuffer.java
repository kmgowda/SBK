/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.data.impl;

import io.sbk.data.DataType;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Class for processing ByteBuffer data.
 */
public class NioByteBuffer implements DataType<ByteBuffer> {

    /**
     * Create byte buffer.
     * @param size size (number of bytes) of the data to create.
     * @return T return the data.
     */
    @Override
    public ByteBuffer allocate(int size) {
        return ByteBuffer.allocateDirect(size);
    }


    /**
     * Create byte buffer data and will with random data.
     * @param size size (number of bytes) of the data to create.
     * @return T return the data.
     */
    @Override
    public ByteBuffer create(int size) {
        Random random = new Random();
        ByteBuffer buffer = allocate(size);
        for (int i = 0; i < size; ++i) {
            buffer.put((byte) (random.nextInt(26) + 65));
        }
        buffer.flip();
        return buffer;
    }

    /**
     * Get the size of the given data in terms of number of bytes, for writers.
     * @param  data data
     * @return return size of the data.
     */
    @Override
    public int length(@NotNull ByteBuffer data) {
        return data.limit();
    }

    /**
     * Set the time for data.
     * @param  data data
     * @param  time time to set
     * @return ByteBuffer return the data.
     */
    @Override
    public ByteBuffer setTime(@NotNull ByteBuffer data, long time) {
        data.putLong(0, time);
        return data;
    }

    /**
     * Get the time of data.
     * @param  data data
     * @return long return the time set by last {@link NioByteBuffer#setTime(ByteBuffer, long)}} )}}.
     */
    @Override
    public long getTime(@NotNull ByteBuffer data) {
        return data.getLong(0);
    }

    /**
     * Get minimum Write and Read Data Size.
     * @return int minimum data size Write and Read.
     */
    @Override
    public int getWriteReadMinSize() {
        return TIME_HEADER_BYTES;
    }

}
