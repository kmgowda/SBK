/**
 * Copyright (c) KMG. All Rights Reserved..
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Memcached;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.data.impl.SbkString;
import io.sbk.params.InputOptions;
import io.sbk.params.ParameterOptions;
import net.rubyeye.xmemcached.XMemcachedClient;

import java.io.IOException;
import java.util.Objects;

/**
 * Class for Memcached storage driver.
 *
 */
public class Memcached implements Storage<String> {
    private final static String CONFIGFILE = "Memcached.properties";
    private MemcachedConfig config;

    private XMemcachedClient xMemCachedclient;

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(Memcached.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    MemcachedConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("url", true, "Memcached server url, default url: " + config.url);
        params.addOption("port", true, "Port, default : " + config.port);
    }

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.url = params.getOptionValue("url", config.url);
        config.port = Integer.parseInt(params.getOptionValue("port", String.valueOf(config.port)));
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        xMemCachedclient = new XMemcachedClient(config.url, config.port);
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        if (!xMemCachedclient.isShutdown()) {
            xMemCachedclient.shutdown();
        }
    }

    @Override
    public DataWriter<String> createWriter(final int id, final ParameterOptions params) {
        return new MemcachedWriter(id, xMemCachedclient);
    }

    @Override
    public DataReader<String> createReader(final int id, final ParameterOptions params) {
        return new MemcachedReader(id, xMemCachedclient);
    }

    @Override
    public DataType<String> getDataType() {
        return new SbkString();
    }
}
