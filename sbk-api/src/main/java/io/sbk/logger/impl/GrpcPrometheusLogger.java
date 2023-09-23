/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.perl.data.Bytes;
import io.perl.config.LatencyConfig;
import io.perl.api.LatencyRecorder;
import io.sbk.action.Action;
import io.perl.exception.ExceptionHandler;
import io.sbp.api.Sbp;
import io.sbp.config.SbpVersion;
import io.sbp.grpc.ClientID;
import io.sbp.grpc.Config;
import io.sbp.grpc.MessageLatenciesRecord;
import io.sbp.grpc.ServiceGrpc;
import io.sbk.logger.SbmHostConfig;
import io.sbk.params.InputOptions;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.sbp.grpc.Version;
import io.time.Time;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public class GrpcPrometheusLogger extends PrometheusLogger {
    private final static String CONFIG_FILE = "sbmhost.properties";
    private final static int LATENCY_MAP_BYTES = 16;

    private SbmHostConfig sbmHostConfig;
    private boolean enable;
    private long clientID;
    private long seqNum;
    private int latencyBytes;
    private int maxLatencyBytes;
    private boolean blocking;
    private LatencyRecorder recorder;

    private AtomicLongArray ramWriteBytesArray;
    private AtomicLongArray ramWriteRequestRecordsArray;
    private AtomicLongArray ramWriteTimeoutEventsArray;
    private AtomicLongArray ramReadBytesArray;
    private AtomicLongArray ramReadRequestRecordsArray;
    private AtomicLongArray ramReadTimeoutEventsArray;
    private ManagedChannel channel;
    private ServiceGrpc.ServiceStub stub;
    private ServiceGrpc.ServiceBlockingStub blockingStub;
    private MessageLatenciesRecord.Builder builder;
    private StreamObserver<com.google.protobuf.Empty> observer;
    private ExceptionHandler exceptionHandler;

    /**
     * calls its super class PrometheusLogger.
     */
    public GrpcPrometheusLogger() {
        super();
        this.ramWriteBytesArray = null;
        this.ramWriteRequestRecordsArray = null;
        this.ramReadBytesArray = null;
        this.ramReadRequestRecordsArray = null;
        this.ramWriteTimeoutEventsArray = null;
        this.ramReadTimeoutEventsArray = null;
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
            sbmHostConfig = mapper.readValue(
                    GrpcPrometheusLogger.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                    SbmHostConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }
        maxLatencyBytes = sbmHostConfig.maxRecordSizeMB * Bytes.BYTES_PER_MB;
        sbmHostConfig.host = DISABLE_STRING;
        params.addOption("sbm", true, "SBM host" +
                "; '" + DISABLE_STRING + "' disables this option, default: " + sbmHostConfig.host);
        params.addOption("sbmport", true, "SBM Port" +
                "; default: " + sbmHostConfig.port);
        //params.addOption("blocking", true, "blocking calls to SBM; default: false");
    }

    @Override
    public void parseArgs(final ParsedOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        sbmHostConfig.host = params.getOptionValue("sbm", sbmHostConfig.host);
        enable = !sbmHostConfig.host.equalsIgnoreCase("no");
        if (!enable) {
            return;
        }
        sbmHostConfig.port = Integer.parseInt(params.getOptionValue("sbmport", Integer.toString(sbmHostConfig.port)));
        //        blocking = Boolean.parseBoolean(params.getOptionValue("blocking", "false"));
        blocking = false;
        exceptionHandler = null;
    }

    @Override
    public void open(final ParsedOptions params, final String storageName, Action action, Time time) throws IllegalArgumentException, IOException {
        super.open(params, storageName, action, time);
        if (!enable) {
            return;
        }
        this.ramWriteBytesArray = new AtomicLongArray(getMaxWriterIDs());
        this.ramWriteRequestRecordsArray = new AtomicLongArray(getMaxWriterIDs());
        this.ramReadBytesArray = new AtomicLongArray(getMaxReaderIDs());
        this.ramReadRequestRecordsArray = new AtomicLongArray(getMaxReaderIDs());
        this.ramWriteTimeoutEventsArray = new AtomicLongArray(getMaxWriterIDs());
        this.ramReadTimeoutEventsArray = new AtomicLongArray(getMaxReaderIDs());
        channel = ManagedChannelBuilder.forTarget(sbmHostConfig.host + ":" + sbmHostConfig.port).usePlaintext().build();
        blockingStub = ServiceGrpc.newBlockingStub(channel);
        try {
            Version sbmSbpVersion = blockingStub.getVersion(Empty.newBuilder().build());
            SbpVersion version = Sbp.getVersion();
            if (version.major != sbmSbpVersion.getMajor()) {
                throw new IllegalArgumentException("SBM SBP Major Version: " + sbmSbpVersion.getMajor() +
                        ", SBK SBP Major Version: " + version.major + " are not same!");
            } else {
                Printer.log.info("SBK SBP Version Major: " + version.major+", Minor: " + version.minor);
                Printer.log.info("SBM SBP Version Major: " + sbmSbpVersion.getMajor() +
                        ", Minor: "+sbmSbpVersion.getMinor());
            }
        } catch (StatusRuntimeException ex) {
            ex.printStackTrace();
            throw new IOException("GRPC get SBP Version failed");
        }

        Config config;
        try {
            config = blockingStub.getConfig(Empty.newBuilder().build());
        } catch (StatusRuntimeException ex) {
            ex.printStackTrace();
            throw new IOException("GRPC GetConfig failed");
        }
        if (!config.getStorageName().equalsIgnoreCase(storageName)) {
            throw new IllegalArgumentException("SBM storage name : " + config.getStorageName()
                    + " ,Supplied storage name: " + storageName + " are not same!");
        }
        if (!config.getAction().name().equalsIgnoreCase(action.name())) {
            throw new IllegalArgumentException("SBM action: " + config.getAction().name()
                    + " ,Supplied action : " + action.name() + " are not same!");
        }
        if (!config.getTimeUnit().name().equalsIgnoreCase(time.getTimeUnit().name())) {
            throw new IllegalArgumentException("SBM Time Unit: " + config.getTimeUnit().name()
                    + " ,Supplied Time Unit : " + time.getTimeUnit().name() + " are not same!");
        }
        if (config.getMinLatency() != getMinLatency()) {
            Printer.log.warn("SBM , min latency : " + config.getMinLatency()
                    + ", local min latency: " + getMinLatency() + " are not same!");
        }
        if (config.getMaxLatency() != getMaxLatency()) {
            Printer.log.warn("SBM, max latency : " + config.getMaxLatency()
                    + ", local max latency: " + getMaxLatency() + " are not same!");
        }
        if (config.getIsReadRequests() !=  isReadRequestsEnabled()) {
            Printer.log.warn("SBM, read request: " + config.getIsReadRequests()
                    + ", local read request: " + isReadRequestsEnabled() + " are not same!" +
                    ", set the option -rq to "+ config.getIsReadRequests());
        }
        if (config.getIsWriteRequests() !=  isWriteRequestsEnabled()) {
            Printer.log.warn("SBM, write request: " + config.getIsWriteRequests()
                    + ", local write request: " + isWriteRequestsEnabled() + " are not same!" +
                    ", set the option -wq to "+config.getIsWriteRequests());
        }

        try {
            clientID = blockingStub.registerClient(config).getId();
        } catch (StatusRuntimeException ex) {
            ex.printStackTrace();
            throw new IOException("GRPC registerClient failed");
        }

        if (clientID < 0) {
            String errMsg = "Invalid client id: " + clientID + " received from SBM Server";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        seqNum = 0;
        latencyBytes = 0;
        recorder = new LatencyRecorder(getMinLatency(), getMaxLatency(), LatencyConfig.LONG_MAX,
                LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX);
        builder = MessageLatenciesRecord.newBuilder();
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
    public void close(final ParsedOptions params) throws IllegalArgumentException, IOException {
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

    /**
     * Sends Latencies Records.
     */
    public void sendLatenciesRecord() {
        long writeRequestsSum = 0;
        long writeBytesSum = 0;
        long readRequestsSum = 0;
        long readBytesSum = 0;
        long writeTimeoutEventsSum = 0;
        long readTimeoutEventsSum = 0;
        for (int i = 0; i < getMaxWriterIDs(); i++) {
            writeRequestsSum += ramWriteRequestRecordsArray.getAndSet(i, 0);
            writeBytesSum += ramWriteBytesArray.getAndSet(i, 0);
            writeTimeoutEventsSum += ramWriteTimeoutEventsArray.getAndSet(i, 0);
        }
        for (int i = 0; i < getMaxReaderIDs(); i++) {
            readRequestsSum += ramReadRequestRecordsArray.getAndSet(i, 0);
            readBytesSum += ramReadBytesArray.getAndSet(i, 0);
            readTimeoutEventsSum += ramReadTimeoutEventsArray.getAndSet(i, 0);
        }
        builder.setWriteRequestBytes(writeBytesSum);
        builder.setWriteRequestRecords(writeRequestsSum);
        builder.setReadRequestBytes(readBytesSum);
        builder.setReadRequestRecords(readRequestsSum);
        builder.setWriteTimeoutEvents(writeTimeoutEventsSum);
        builder.setReadTimeoutEvents(readTimeoutEventsSum);
        builder.setClientID(clientID);
        builder.setSequenceNumber(++seqNum);
        builder.setMaxReaders(getMaxReadersCount());
        builder.setReaders(getReadersCount());
        builder.setWriters(getWritersCount());
        builder.setMaxWriters(getMaxWritersCount());
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

    @Override
    public void recordWriteRequests(int writerId, long startTime, long bytes, long events) {
        super.recordWriteRequests(writerId, startTime, bytes, events);
        if (enable) {
            ramWriteRequestRecordsArray.addAndGet(writerId, events);
            ramWriteBytesArray.addAndGet(writerId, bytes);
        }
    }

    @Override
    public void recordReadRequests(int readerId, long startTime, long bytes, long events) {
        super.recordReadRequests(readerId, startTime, bytes, events);
        if (enable) {
            ramReadRequestRecordsArray.addAndGet(readerId, events);
            ramReadBytesArray.addAndGet(readerId, bytes);
        }
    }
    
    @Override
    public void recordWriteTimeoutEvents(int readerId, long startTime, long events) {
        super.recordWriteTimeoutEvents(readerId, startTime, events);
        if (enable) {
            ramWriteTimeoutEventsArray.addAndGet(readerId, events);
        }
    }

    @Override
    public void recordReadTimeoutEvents(int writerId, long startTime, long events) {
        super.recordReadTimeoutEvents(writerId, startTime, events);
        if (enable) {
            ramReadTimeoutEventsArray.addAndGet(writerId, events);
        }
    }

    /**
     * record every latency.
     */
    @Override
    public void recordLatency(long startTime, int events, int bytes, long latency) {
        if (!enable) {
            return;
        }

        if (latencyBytes >= maxLatencyBytes) {
            sendLatenciesRecord();
        }
        if (recorder.record(events, bytes, latency)) {
            final Long cnt = builder.getLatencyMap().getOrDefault(latency, 0L);
            builder.putLatency(latency, cnt + events);
            if (cnt == 0) {
                latencyBytes += LATENCY_MAP_BYTES;
            }
        }
    }

    @Override
    public void print(int writers, int maxWriters, int readers, int maxReaders,
                      long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                      double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                      long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
                      long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                      long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                      long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                      long readTimeoutEvents, double readTimeoutEventsPerSec,
                      double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                      double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                      long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        super.print(writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestMbPerSec, writeRequestRecords,
                writeRequestRecordsPerSec, readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileValues);
        if (enable) {
            sendLatenciesRecord();
        }
    }

    private static class ResponseObserver<T> implements StreamObserver<T> {

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

}
