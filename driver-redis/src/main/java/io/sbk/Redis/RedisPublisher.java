/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Redis;


import io.sbk.parameters.ParameterOptions;
import io.sbk.api.Writer;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Redis Publisher.
 */
public class RedisPublisher implements Writer<String> {
    final private Jedis jedis;
    final private String channelName;

    public RedisPublisher(int id, ParameterOptions params, Jedis jedis, String channelName) throws IOException {
        this.jedis = jedis;
        this.channelName = channelName;
    }


    @Override
    public CompletableFuture writeAsync(String data) throws IOException {
        jedis.publish(channelName, data);
        return null;
    }

    @Override
    public void close() throws  IOException {
    }
}