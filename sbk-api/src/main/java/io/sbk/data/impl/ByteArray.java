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
 * Class for processing byte[] data.
 */
final public class ByteArray implements DataType<byte[]> {

    /**
     * Create byte array data.
     *
     * @param size size (number of bytes) of the data to create.
     * @return T return the data.
     */
    @Override
    public byte[] allocate(int size) {
        return new byte[size];
    }

    /**
     * Create byte array data and fill the random data.
     *
     * @param size size (number of bytes) of the data to create.
     * @return T return the data.
     */
    @Override
    public byte[] create(int size) {
        final Random random = new Random();
        byte[] bytes = allocate(size);
        for (int i = 0; i < size; ++i) {
            bytes[i] = (byte) (random.nextInt(26) + 65);
        }
        return bytes;
    }

    /**
     * Get the size of the given data in terms of number of bytes for writers.
     *
     * @param data data
     * @return return size of the data.
     */
    @Override
    public int length(@NotNull byte[] data) {
        return data.length;
    }

    /**
     * Set the time for data.
     *
     * @param data data
     * @param time time to set
     * @return byte[] return the data.
     */
    @Override
    public byte[] setTime(byte[] data, long time) {
        byte[] bytes = ByteBuffer.allocate(TIME_HEADER_BYTES).putLong(0, time).array();
        System.arraycopy(bytes, 0, data, 0, TIME_HEADER_BYTES);
        return data;
    }

    /**
     * Get the time of data.
     *
     * @param data data
     * @return long return the time set by last {@link ByteArray#setTime(byte[], long)}} )}}.
     */
    @Override
    public long getTime(byte[] data) {
        return ByteBuffer.allocate(TIME_HEADER_BYTES).put(data, 0, TIME_HEADER_BYTES).getLong(0);
    }

    /**
     * Get minimum Write and Read Data Size.
     *
     * @return int minimum data size Write and Read.
     */
    @Override
    public int getWriteReadMinSize() {
        return TIME_HEADER_BYTES;
    }
}
