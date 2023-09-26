/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Redis;

import io.sbk.params.ParameterOptions;
import io.sbk.api.Reader;
import redis.clients.jedis.Jedis;

import java.io.EOFException;
import java.io.IOException;

public class RedisReader implements Reader<String> {
    final private Jedis jedis;
    final private String listName;

    public RedisReader(int id, ParameterOptions params, Jedis jedis, String listName) throws IOException {
        this.jedis = jedis;
        this.listName = listName;
    }


    @Override
    public String read() throws IOException, EOFException {
        String ret = jedis.lpop(listName);
        if (ret == null) {
            throw new EOFException();
        }
        return ret;
    }


    @Override
    public void close() throws IOException {

    }
}
