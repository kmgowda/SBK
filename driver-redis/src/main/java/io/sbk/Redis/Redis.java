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
import io.sbk.api.DataReader;
import io.sbk.api.DataType;
import io.sbk.api.DataWriter;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;

import java.io.IOException;

import io.sbk.api.impl.SbkLogger;
import io.sbk.api.impl.StringHandler;
import redis.clients.jedis.Jedis;


/**
 * Class for Redis List.
 */
public class Redis implements Storage<String> {
    private Jedis jedis;
    private Jedis jedisConsumer;
    private String listName;
    private String serverUri;

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        params.addOption("list", true, "List /Channel name");
        params.addOption("uri", true, "Server URI");
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        listName =  params.getOptionValue("list", "list-1");
        serverUri = params.getOptionValue("uri", "localhost");
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        jedis = new Jedis(serverUri);
        if (params.isWriteAndRead()) {
            jedisConsumer = new Jedis(serverUri);
        }
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {
        jedis.close();
        if (params.isWriteAndRead()) {
            jedisConsumer.close();
        }
    }

    @Override
    public DataWriter<String> createWriter(final int id, final Parameters params) {
        try {
            if (params.isWriteAndRead()) {
                SbkLogger.log.info("Starting Redis Publisher : "+id);
                return new RedisPublisher(id, params, jedis, listName);
            } else {
                SbkLogger.log.info("Starting Redis Writer : "+id);
                return new RedisWriter(id, params, jedis, listName);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<String> createReader(final int id, final Parameters params) {
        try {
            if (params.isWriteAndRead()) {
                SbkLogger.log.info("Starting Redis Consumer : "+id);
                return new RedisConsumer(id, params, jedisConsumer, listName);
            } else {
                SbkLogger.log.info("Starting Redis Reader : "+id);
                return new RedisReader(id, params, jedis, listName);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataType<String> getDataType() {
        return new StringHandler();
    }
}
