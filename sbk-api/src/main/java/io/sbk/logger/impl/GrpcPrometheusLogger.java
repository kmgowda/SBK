/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.sbk.action.Action;
import io.sbk.exception.ExceptionHandler;
import io.sbk.options.InputOptions;
import io.sbk.logger.RamHostConfig;
import io.sbk.grpc.ClientID;
import io.sbk.grpc.Config;
import io.sbk.grpc.LatenciesRecord;
import io.sbk.grpc.ServiceGrpc;
import io.sbk.perl.LatencyRecorder;
import io.sbk.config.PerlConfig;
import io.sbk.time.Time;
import io.sbk.system.Printer;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public class GrpcPrometheusLogger extends PrometheusLogger {
    final static String CONFIG_FILE = "ramhost.properties";
    final static int LATENCY_MAP_BYTES = 16;

    public RamHostConfig ramHostConfig;
    private boolean enable;
    private long clientID;
    private long seqNum;
    private int latencyBytes;
    private int maxLatencyBytes;
    private boolean blocking;
    private LatencyRecorder recorder;
    private ManagedChannel channel;
    private ServiceGrpc.ServiceStub stub;
    private ServiceGrpc.ServiceBlockingStub blockingStub;
    private LatenciesRecord.Builder builder;
    private StreamObserver<com.google.protobuf.Empty> observer;
    private ExceptionHandler exceptionHandler;

    public GrpcPrometheusLogger() {
        super();
    }


    private class ResponseObserver<T> implements StreamObserver<T> {

        @Override
        public void onNext(Object value) {

        }

        @Override
        public void onError(Throwable ex) {
                // graceful exit may not work GRPC
                Runtime.getRuntime().exit(1);
        }

        @Override
        public void onCompleted() {

        }
    }


    @Override
    public void setExceptionHandler(ExceptionHandler handler) {
        this.exceptionHandler = handler;
    }


    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        super.addArgs(params);
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            ramHostConfig = mapper.readValue(
                    GrpcPrometheusLogger.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                    RamHostConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }
        maxLatencyBytes = ramHostConfig.maxRecordSizeMB * PerlConfig.BYTES_PER_MB;
        ramHostConfig.host = DISABLE_STRING;
        params.addOption("ram", true, "SBK RAM host" +
                "; '" +DISABLE_STRING+"' disables this option, default: " + ramHostConfig.host);
        params.addOption("ramport", true, "SBK RAM Port" +
                "; default: " + ramHostConfig.port );
        //params.addOption("blocking", true, "blocking calls to SBK RAM; default: false");
    }


    @Override
    public void parseArgs(final InputOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        ramHostConfig.host = params.getOptionValue("ram", ramHostConfig.host );
        enable = !ramHostConfig.host.equalsIgnoreCase("no");
        if (!enable) {
            return;
        }
        ramHostConfig.port = Integer.parseInt(params.getOptionValue("ramport", Integer.toString(ramHostConfig.port)));
        //        blocking = Boolean.parseBoolean(params.getOptionValue("blocking", "false"));
        blocking = false;
        exceptionHandler = null;
    }


    @Override
    public void open(final InputOptions params, final String storageName, Action action, Time time) throws IllegalArgumentException, IOException {
        super.open(params, storageName, action, time);
        if (!enable) {
            return;
        }
        channel = ManagedChannelBuilder.forTarget(ramHostConfig.host+":"+ ramHostConfig.port).usePlaintext().build();
        blockingStub = ServiceGrpc.newBlockingStub(channel);
        Config config;
        try {
            config = blockingStub.getConfig(Empty.newBuilder().build());
        } catch (StatusRuntimeException ex) {
            ex.printStackTrace();
            throw new IOException("GRPC GetConfig failed");
        }
        if (!config.getStorageName().equalsIgnoreCase(storageName)) {
            throw new IllegalArgumentException("SBK RAM storage name : "+config.getStorageName()
                    + " ,Supplied storage name: "+storageName +" are not same!");
        }
        if (!config.getAction().name().equalsIgnoreCase(action.name())) {
            throw new IllegalArgumentException("SBK RAM action: "+config.getAction().name()
                    + " ,Supplied action : "+action.name() +" are not same!");
        }
        if (!config.getTimeUnit().name().equalsIgnoreCase(time.getTimeUnit().name())) {
            throw new IllegalArgumentException("SBK RAM Time Unit: "+config.getTimeUnit().name()
                    + " ,Supplied Time Unit : "+time.getTimeUnit().name() +" are not same!");
        }
        if (config.getMinLatency() != getMinLatency()) {
            Printer.log.warn("SBK RAM , min latency : "+config.getMinLatency()
                    +", local min latency: "+getMinLatency() +" are not same!");
        }
        if (config.getMaxLatency() != getMaxLatency()) {
            Printer.log.warn("SBK RAM , min latency : "+config.getMaxLatency()
                    +", local min latency: "+getMaxLatency() +" are not same!");
        }
        try {
            clientID = blockingStub.registerClient(config).getId();
        } catch (StatusRuntimeException ex) {
            ex.printStackTrace();
            throw new IOException("GRPC registerClient failed");
        }

        if (clientID < 0) {
            String errMsg = "Invalid client id: "+clientID+" received from SBK RAM";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        seqNum = 0;
        latencyBytes = 0;
        recorder = new LatencyRecorder(getMinLatency(), getMaxLatency(), PerlConfig.LONG_MAX,
                PerlConfig.LONG_MAX, PerlConfig.LONG_MAX);
        builder = LatenciesRecord.newBuilder();
        if (blocking) {
          stub = null;
          observer = null;
        } else {
            stub = ServiceGrpc.newStub(channel);
            observer = new ResponseObserver<>();
        }
        Printer.log.info("SBK GRPC Logger Started");
    }

    @Override
    public void close(final InputOptions params) throws IllegalArgumentException, IOException  {
        super.close(params);
        if (!enable) {
            return;
        }
        try {
            builder.clear();
            blockingStub.closeClient(ClientID.newBuilder().setId(clientID).build());
            channel.shutdownNow().awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        Printer.log.info("SBK GRPC Logger Shutdown");
    }

    public void sendLatenciesRecord() {
        builder.setClientID(clientID);
        builder.setSequenceNumber(++seqNum);
        builder.setMaxReaders(maxReaders.get());
        builder.setReaders(readers.get());
        builder.setWriters(writers.get());
        builder.setMaxWriters(maxWriters.get());
        builder.setMaxLatency(recorder.getMaxLatency());
        builder.setTotalLatency(recorder.getTotalLatency());
        builder.setInvalidLatencyRecords(recorder.getInvalidLatencyRecords());
        builder.setTotalBytes(recorder.getTotalBytes());
        builder.setTotalRecords(recorder.getTotalRecords());
        builder.setHigherLatencyDiscardRecords(recorder.getHigherLatencyDiscardRecords());
        builder.setLowerLatencyDiscardRecords(recorder.getLowerLatencyDiscardRecords());
        builder.setValidLatencyRecords(recorder.getValidLatencyRecords());
        if (stub != null) {
            stub.addLatenciesRecord(builder.build(), observer);
        } else {
            blockingStub.addLatenciesRecord(builder.build());
        }
        recorder.reset();
        builder.clear();
        latencyBytes = 0;
    }


    /**
     *  record every latency.
     */
    @Override
    public void recordLatency(long startTime, int bytes, int events, long latency) {
        if (!enable) {
            return;
        }

        if (latencyBytes >= maxLatencyBytes) {
            sendLatenciesRecord();
        }
        if (recorder.record(bytes, events, latency)) {
            final Long cnt = builder.getLatencyMap().getOrDefault(latency, 0L);
            builder.putLatency(latency, cnt + events);
            if (cnt == 0) {
                latencyBytes += LATENCY_MAP_BYTES;
            }
        }
    }

    @Override
    public void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                      long slc1, long slc2, long[] percentileValues) {
        super.print(seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileValues);
        if (latencyBytes > 0 ) {
            sendLatenciesRecord();
        }
    }

}
