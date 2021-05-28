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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.sbk.api.Action;
import io.sbk.api.InputOptions;
import io.sbk.api.ServerConfig;
import io.sbk.perl.LatencyRecord;
import io.sbk.perl.Time;
import io.sbk.system.Printer;
import java.io.IOException;
import java.util.concurrent.TimeUnit;



/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public class SbkGrpcPrometheusLogger extends SbkPrometheusLogger {
    final static String CONFIG_FILE = "server.properties";
    final static String DISABLE_STRING = "no";
    final static int MAX_LATENCY_BYTES = 1024 * 1024 * 4;
    final static int LATENCY_BYTES = 16;
    final static int MAX_LATENCY_ITERATIONS = MAX_LATENCY_BYTES / LATENCY_BYTES;
    public ServerConfig serverConfig;
    private boolean enable;
    private int clientID;
    private long transID;
    private long seqNum;
    private int  latenciesCount;
    private ManagedChannel channel;
    private SBKServiceGrpc.SBKServiceStub stub;
    private LatenciesList.Builder listBuilder;

    public SbkGrpcPrometheusLogger() {
        super();
    }


    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        super.addArgs(params);
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            serverConfig = mapper.readValue(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                    ServerConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }
        serverConfig.host = DISABLE_STRING;
        params.addOption("sbkserver", true, "SBK Server host" +
                "; default host: " + serverConfig.host +" ; disable if this parameter is set to: " +DISABLE_STRING);
        params.addOption("sbkport", true, "SBK Server Port" +
                "; default port: " + serverConfig.port );
    }


    @Override
    public void parseArgs(final InputOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        serverConfig.host = params.getOptionValue("sbkserver", serverConfig.host );
        serverConfig.port = Integer.parseInt(params.getOptionValue("sbkport", Integer.toString(serverConfig.port)));
        enable = !serverConfig.host.equalsIgnoreCase("no");
    }


    @Override
    public void open(final InputOptions params, final String storageName, Action action, Time time) throws IllegalArgumentException, IOException {
        super.open(params, storageName, action, time);
        if (!enable) {
            return;
        }
        channel = ManagedChannelBuilder.forTarget(serverConfig.host+":"+serverConfig.port).usePlaintext().build();
        final SBKServiceGrpc.SBKServiceBlockingStub blockingStub = SBKServiceGrpc.newBlockingStub(channel);
        Config config;
        try {
            config = blockingStub.getConfig(Empty.newBuilder().build());
        } catch (StatusRuntimeException ex) {
            ex.printStackTrace();
            throw new IOException("GRPC GetConfig failed");
        }
        if (!config.getStorageName().equalsIgnoreCase(storageName)) {
            throw new IllegalArgumentException("SBK Server storage name : "+config.getStorageName()
                    + " ,Supplied storage name: "+storageName +" are not same!");
        }
        if (!config.getAction().name().equalsIgnoreCase(action.name())) {
            throw new IllegalArgumentException("SBK Server action: "+config.getAction().name()
                    + " ,Supplied action : "+action.name() +" are not same!");
        }
        if (config.getTimeUnit().name().equalsIgnoreCase(time.getTimeUnit().name())) {
            throw new IllegalArgumentException("SBK Server Time Unit: "+config.getTimeUnit().name()
                    + " ,Supplied Time Unit : "+time.getTimeUnit().name() +" are not same!");
        }
        if (config.getMinLatency() != getMinLatency()) {
            Printer.log.warn("SBK Server , min latency : "+config.getMinLatency()
                    +", local min latency: "+getMinLatency() +" are not same!");
        }
        if (config.getMaxLatency() != getMaxLatency()) {
            Printer.log.warn("SBK Server , min latency : "+config.getMaxLatency()
                    +", local min latency: "+getMaxLatency() +" are not same!");
        }
        try {
            clientID = blockingStub.registerClient(config).getId();
        } catch (StatusRuntimeException ex) {
            ex.printStackTrace();
            throw new IOException("GRPC registerClient failed");
        }

        if (clientID < 0) {
            String errMsg = "Invalid client id: "+clientID+" received from SBK Server";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        stub = SBKServiceGrpc.newStub(channel);
        transID = 0;
        seqNum = 0;
        listBuilder = LatenciesList.newBuilder();
        Printer.log.info("SBK GRPC Logger Started");
    }

    @Override
    public void close(final InputOptions params) throws IllegalArgumentException, IOException  {
        super.close(params);
        try {
            listBuilder.clear();
            stub.closeClient(ClientID.newBuilder().setId(clientID).build(), null);
            channel.shutdownNow().awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        Printer.log.info("SBK GRPC Logger Shutdown");
    }

    /**
     * open the reporting window.
     */
    public void openWindow() {
        seqNum = 0;
        latenciesCount = 0;
        Transaction.Builder builder = Transaction.newBuilder();
        builder.setClientID(clientID);
        builder.setTransID(++transID);
        builder.setSeqNum(seqNum++);
        stub.startTransaction(builder.build(), null);
    }

    /**
     * close the reporting window.
     */
    public void closeWindow() {
        if (latenciesCount > 0) {
            stub.addLatenciesList(listBuilder.build(), null);
        }
        listBuilder.clear();
        latenciesCount = 0;
        Transaction.Builder builder = Transaction.newBuilder();
        builder.setClientID(clientID);
        builder.setTransID(transID);
        builder.setSeqNum(seqNum++);
        stub.endTransaction(builder.build(), null);
    }

    /**
     * Report a latency Record.
     *
     * @param record Latency Record
     */
    public void reportLatencyRecord(LatencyRecord record) {
        LatenciesRecord.Builder builder = LatenciesRecord.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        transBuilder.setClientID(clientID);
        transBuilder.setTransID(transID);
        transBuilder.setSeqNum(seqNum++);
        builder.setTransaction(transBuilder.build());
        builder.setTotalBytes(record.totalBytes);
        builder.setTotalRecords(record.totalRecords);
        builder.setTotalLatency(record.totalLatency);
        builder.setMaxLatency(record.maxLatency);
        builder.setHigherLatencyDiscardRecords(record.higherLatencyDiscardRecords);
        builder.setLowerLatencyDiscardRecords(record.lowerLatencyDiscardRecords);
        builder.setInvalidLatencyRecords(record.invalidLatencyRecords);
        builder.setValidLatencyRecords(record.validLatencyRecords);
        builder.setWriters(writers.get());
        builder.setMaxLatency(maxWriters.get());
        builder.setReaders(readers.get());
        builder.setMaxReaders(maxReaders.get());
        stub.addLatenciesRecord(builder.build(), null);
    }

    /**
     * Report one latency .
     *
     * @param latency Latency value
     * @param count  Number of times the latency value is observed
     */
    public void reportLatency(long latency, long count) {
        if (latenciesCount >= MAX_LATENCY_ITERATIONS) {
            stub.addLatenciesList(listBuilder.build(), null);
            listBuilder.clear();
            latenciesCount = 0;
        }
        if (latenciesCount == 0) {
            Transaction.Builder transBuilder = Transaction.newBuilder();
            transBuilder.setClientID(clientID);
            transBuilder.setTransID(transID);
            transBuilder.setSeqNum(seqNum++);
            listBuilder.setTransaction(transBuilder.build());
        }
        listBuilder.putLatencies(latency, count);
        latenciesCount++;
    }

}
