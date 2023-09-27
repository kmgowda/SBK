/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.Redis;

import io.sbk.api.AbstractCallbackReader;
import io.sbk.api.Callback;
import io.sbk.params.ParameterOptions;
import io.sbk.system.Printer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;

/**
 * Class for RabbitMQ Callback Reader.
 */
public class RedisConsumer extends AbstractCallbackReader<String> {
    final private Jedis jedis;
    final private String channelName;

    public RedisConsumer(int id, ParameterOptions params, Jedis jedis, String channelName) throws IOException {
        this.jedis = jedis;
        this.channelName = channelName;
    }

    @Override
    public void start(Callback<String> callback) throws IOException {
        try {
            jedis.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    callback.consume(message);
                }
            }, channelName);
        } catch (JedisConnectionException ex) {
            Printer.log.warn(ex.toString());
        }
    }

    @Override
    public void stop() throws IOException {
    }
}