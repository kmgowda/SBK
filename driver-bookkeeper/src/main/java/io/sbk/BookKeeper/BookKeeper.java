/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.BookKeeper;

import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.Storage;
import io.sbk.parameters.ParameterOptions;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.distributedlog.DistributedLogConfiguration;
import org.apache.distributedlog.api.DistributedLogManager;
import org.apache.distributedlog.api.namespace.Namespace;
import org.apache.distributedlog.api.namespace.NamespaceBuilder;

/**
 * Class for Bookkeeper Benchmarking.
 */
public class BookKeeper implements Storage<byte[]> {
    private final static String CONFIGFILE = "BookKeeper.properties";
    private Namespace namespace;
    private URI dlogUri;
    private DistributedLogConfiguration conf;
    private DistributedLogManager dlm;
    private String logName;
    private String uriName;
    private boolean recreate;
    private int ensembleSize;
    private int writeQuorum;
    private int ackQuorum;

    @Override
    public void addArgs(final ParameterOptions params) throws IllegalArgumentException {
        params.addOption("log", true, "Log name");
        params.addOption("uri", true, "URI");
        params.addOption("ensembleSize", true, "EnsembleSize (default value is in "+CONFIGFILE+" )");
        params.addOption("writeQuorum", true, "WriteQuorum  (default value is in "+CONFIGFILE+" )");
        params.addOption("ackQuorum", true, "AckQuorum (default value is in "+CONFIGFILE+" )");
        params.addOption("recreate", true,
                "If the log is already existing, delete and recreate the same (default: false)");
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        recreate = false;
        logName =  params.getOptionValue("log", null);
        uriName = params.getOptionValue("uri", null);
        if (uriName == null) {
            throw new IllegalArgumentException("Error: Must specify Bookkeeper/Distributed log IP address");
        }
        if (logName == null) {
            throw new IllegalArgumentException("Error: Must specify Log Name");
        }
        PropertiesConfiguration propsConf = new PropertiesConfiguration();
        try {
            propsConf.load(getClass().getClassLoader().getResourceAsStream(CONFIGFILE));
        } catch (ConfigurationException ex) {
            ex.printStackTrace();
            throw  new IllegalArgumentException(ex);
        }
        conf = new DistributedLogConfiguration();
        conf.loadConf(propsConf);
        conf.setReadAheadWaitTime(params.getTimeoutMS());
        if (params.hasOption("recreate")) {
            recreate = Boolean.parseBoolean(params.getOptionValue("recreate"));
        } else {
            recreate = params.getWritersCount() > 0 && params.getReadersCount() > 0;
        }
        ensembleSize = Integer.parseInt(params.getOptionValue("ensembleSize", String.valueOf(conf.getEnsembleSize())));
        writeQuorum = Integer.parseInt(params.getOptionValue("writeQuorum", String.valueOf(conf.getWriteQuorumSize())));
        ackQuorum = Integer.parseInt(params.getOptionValue("ackQuorum", String.valueOf(conf.getAckQuorumSize())));
        dlogUri = URI.create(uriName);
    }

    @Override
    public void openStorage(final ParameterOptions params) throws  IOException {
        namespace = NamespaceBuilder.newBuilder()
                .conf(conf)
                .uri(dlogUri)
                .build();
        if (recreate) {
            namespace.deleteLog(logName);
        }
        namespace.createLog(logName);
        dlm = namespace.openLog(logName);
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        if (null != namespace) {
            dlm.close();
            namespace.close();
        }
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final ParameterOptions params) {
        try {
            return new BookKeeperWriter(dlm);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        try {
            return new BookKeeperReader(dlm);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}