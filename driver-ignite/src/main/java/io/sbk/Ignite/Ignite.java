/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Ignite;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;
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
    private final static String CONFIGFILE = "ignite.properties";
    private IgniteConfig config;
    private IgniteCache<Long, byte[]> cache;
    private ClientCache<Long, byte[]> clientCache;
    private IgniteClient igniteClient;
    private org.apache.ignite.Ignite ignite;

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(Ignite.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    IgniteConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("url", true, "server address, default : "+ config.url);
        params.addOption("cfile", true, "cluster file");
        params.addOption("cache", true, "cache name,  default : "+ config.cacheName);
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        config.url =  params.getOptionValue("url", config.url);
        config.cacheName = params.getOptionValue("cache", config.cacheName);
        if (params.hasOption("cfile")) {
            config.cFile =  params.getOptionValue("cfile", config.cFile);
        } else {
            config.cFile = null;
        }
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        if (config.cFile == null) {
            ClientConfiguration cfg = new ClientConfiguration().setAddresses(config.url);
            igniteClient = Ignition.startClient(cfg);
            clientCache = igniteClient.getOrCreateCache(config.cacheName);
            ignite = null;
            cache = null;
        }  else {
            ignite = Ignition.start(config.cFile);
            cache = ignite.getOrCreateCache(config.cacheName);
            igniteClient = null;
            clientCache = null;
        }
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {
        if (cache != null) {
            cache.close();
        }
        if (ignite != null) {
            ignite.close();
        }
        if (igniteClient != null) {
            try {
                igniteClient.close();
            } catch (Exception ex) {
                throw  new IOException(ex);
            }
        }
    }

    @Override
    public Writer<byte[]> createWriter(final int id, final Parameters params) {
        try {
            if (cache != null) {
                if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 1) {
                    return new IgniteTransactionWriter(id, params, cache, ignite);
                } else {
                    return new IgniteWriter(id, params, cache);
                }
            } else {
                if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 1) {
                    return new IgniteClientTransactionWriter(id, params, clientCache, igniteClient);
                } else {
                    return new IgniteClientWriter(id, params, clientCache);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader<byte[]> createReader(final int id, final Parameters params) {
        try {
            if (cache != null) {
                if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 1) {
                    return new IgniteTransactionReader(id, params, cache, ignite);
                } else {
                    return new IgniteReader(id, params, cache);
                }
            } else {
                if (params.getRecordsPerSync() < Integer.MAX_VALUE && params.getRecordsPerSync() > 1) {
                    return new IgniteClientTransactionReader(id, params, clientCache, igniteClient);
                } else {
                    return new IgniteClientReader(id, params, clientCache);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static long generateStartKey(int id) {
        return (long) id * (long) Integer.MAX_VALUE;
    }
}
