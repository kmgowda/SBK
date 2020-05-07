/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;

import io.sbk.api.DataType;
import java.util.Random;

/**
 * Class for processing byte[] data.
 */
public class StringHandler implements DataType<String> {
    final static int TIME_HEADER_SIZE = 22;
    final static String FORMAT_STRING = "%0"+TIME_HEADER_SIZE+"d";

    /**
     * Create byte array data.
     * @param size size (number of bytes) of the data to create.
     * @return T return the data.
     */
    @Override
    public String allocate(int size) {
        return create(size);
    }

    /**
     * Create byte array data and fill the random data.
     * @param size size (number of bytes) of the data to create.
     * @return T return the data.
     */
    @Override
    public String create(int size) {
        Random random = new Random();
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; ++i) {
            bytes[i] = (byte) (random.nextInt(26) + 65);
        }
        return new String(bytes);
    }

    /**
     * Get the size of the given data in terms of number of bytes for writers.
     * @param  data data
     * @return return size of the data.
     */
    @Override
    public int length(String data) {
        return data.length();
    }

    /**
     * Set the time for data.
     * @param  data data
     * @param  time time to set
     * @return return the data.
     */
    @Override
    public String setTime(String data, long time) {
        final String timeString = String.format(FORMAT_STRING, time);
        return timeString + data.substring(TIME_HEADER_SIZE);
    }

    /**
     * Get the time of data.
     * @param  data data
     * @return long return the time set by last {@link StringHandler#setTime(String, long)}} )}}.
     */
    @Override
    public long getTime(String data) {
        return Long.parseLong(data.substring(0, TIME_HEADER_SIZE));
    }
}
