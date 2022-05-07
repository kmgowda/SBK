/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Ignite;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.options.InputOptions;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;

import java.io.IOException;
import java.util.Objects;

/**
 * Class for Ignite Benchmarking.
 */
public class Ignite implements Storage<byte[]> {
    private final static String CONFIGFILE = "sbk-ignite.properties";
    private IgniteConfig config;
    private IgniteCache<Long, byte[]> cache;
    private ClientCache<Long, byte[]> clientCache;
    private IgniteClient igniteClient;
    private org.apache.ignite.Ignite ignite;

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(Ignite.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    IgniteConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("url", true, "server address, default : " + config.url);
        params.addOption("cfile", true, "cluster file");
        params.addOption("cache", true, "cache name,  default : " + config.cacheName);
        params.addOption("client", true, "client mode, default: " + config.isClient);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.url = params.getOptionValue("url", config.url);
        config.cacheName = params.getOptionValue("cache", config.cacheName);
        if (params.hasOptionValue("cfile")) {
            config.cFile = params.getOptionValue("cfile", config.cFile);
        } else {
            config.cFile = null;
        }
        config.isClient = Boolean.parseBoolean(params.getOptionValue("client", Boolean.toString(config.isClient)));
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        if (config.isClient) {
            ClientConfiguration cfg = new ClientConfiguration().setAddresses(config.url);
            igniteClient = Ignition.startClient(cfg);
            clientCache = igniteClient.getOrCreateCache(config.cacheName);
            ignite = null;
            cache = null;
            if (params.getWritersCount() > 0) {
                clientCache.clear();
            }
        } else {
            if (config.cFile != null) {
                ignite = Ignition.start(config.cFile);
            } else {
                ignite = Ignition.start();
            }
            igniteClient = null;
            clientCache = null;

            if (params.getWritersCount() > 0) {
                cache = ignite.getOrCreateCache(config.cacheName);
                cache.destroy();
                cache.close();
            }
        }
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        if (ignite != null) {
            ignite.close();
        }
        if (igniteClient != null) {
            try {
                igniteClient.close();
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final ParameterOptions params) {
        try {
            if (config.isClient) {
                if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 1) {
                    return new IgniteClientTransactionWriter(id, params, clientCache, igniteClient);
                } else {
                    return new IgniteClientWriter(id, params, clientCache);
                }
            } else {
                if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 1) {
                    return new IgniteTransactionWriter(id, params, cache, ignite);
                } else {
                    return new IgniteWriter(id, params, ignite, config);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        try {
            if (config.isClient) {
                if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 1) {
                    return new IgniteClientTransactionReader(id, params, clientCache, igniteClient);
                } else {
                    return new IgniteClientReader(id, params, clientCache);
                }
            } else {
                if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 1) {
                    return new IgniteTransactionReader(id, params, cache, ignite);
                } else {
                    return new IgniteReader(id, params, ignite, config);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
