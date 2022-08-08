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
import io.sbp.grpc.ClientID;
import io.sbp.grpc.Config;
import io.sbp.grpc.LatenciesRecord;
import io.sbp.grpc.ServiceGrpc;
import io.sbk.logger.RamHostConfig;
import io.sbk.params.InputOptions;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.time.Time;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public class GrpcPrometheusLogger extends PrometheusLogger {
    private final static String CONFIG_FILE = "ramhost.properties";
    private final static int LATENCY_MAP_BYTES = 16;

    /**
     * <code>Creating RamHostConfig ramHostConfig</code>.
     */
    private RamHostConfig ramHostConfig;
    private boolean enable;
    private long clientID;
    private long seqNum;
    private int latencyBytes;
    private int maxLatencyBytes;
    private boolean blocking;
    private LatencyRecorder recorder;

    private AtomicLongArray ramWriteBytesArray;
    private AtomicLongArray ramWriteRequestsArray;
    private AtomicLongArray ramReadBytesArray;
    private AtomicLongArray ramReadRequestsArray;

    private ManagedChannel channel;
    private ServiceGrpc.ServiceStub stub;
    private ServiceGrpc.ServiceBlockingStub blockingStub;
    private LatenciesRecord.Builder builder;
    private StreamObserver<com.google.protobuf.Empty> observer;
    private ExceptionHandler exceptionHandler;

    /**
     * calls its super class PrometheusLogger.
     */
    public GrpcPrometheusLogger() {
        super();
        this.ramWriteBytesArray = null;
        this.ramWriteRequestsArray = null;
        this.ramReadBytesArray = null;
        this.ramReadRequestsArray = null;
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
        maxLatencyBytes = ramHostConfig.maxRecordSizeMB * Bytes.BYTES_PER_MB;
        ramHostConfig.host = DISABLE_STRING;
        params.addOption("ram", true, "SBK RAM host" +
                "; '" + DISABLE_STRING + "' disables this option, default: " + ramHostConfig.host);
        params.addOption("ramport", true, "SBK RAM Port" +
                "; default: " + ramHostConfig.port);
        //params.addOption("blocking", true, "blocking calls to SBK RAM; default: false");
    }

    @Override
    public void parseArgs(final ParsedOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        ramHostConfig.host = params.getOptionValue("ram", ramHostConfig.host);
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
    public void open(final ParsedOptions params, final String storageName, Action action, Time time) throws IllegalArgumentException, IOException {
        super.open(params, storageName, action, time);
        if (!enable) {
            return;
        }
        this.ramWriteBytesArray = new AtomicLongArray(maxWriterRequestIds);
        this.ramWriteRequestsArray = new AtomicLongArray(maxWriterRequestIds);
        this.ramReadBytesArray = new AtomicLongArray(maxReaderRequestIds);
        this.ramReadRequestsArray = new AtomicLongArray(maxReaderRequestIds);
        channel = ManagedChannelBuilder.forTarget(ramHostConfig.host + ":" + ramHostConfig.port).usePlaintext().build();
        blockingStub = ServiceGrpc.newBlockingStub(channel);
        Config config;
        try {
            config = blockingStub.getConfig(Empty.newBuilder().build());
        } catch (StatusRuntimeException ex) {
            ex.printStackTrace();
            throw new IOException("GRPC GetConfig failed");
        }
        if (!config.getStorageName().equalsIgnoreCase(storageName)) {
            throw new IllegalArgumentException("SBK RAM storage name : " + config.getStorageName()
                    + " ,Supplied storage name: " + storageName + " are not same!");
        }
        if (!config.getAction().name().equalsIgnoreCase(action.name())) {
            throw new IllegalArgumentException("SBK RAM action: " + config.getAction().name()
                    + " ,Supplied action : " + action.name() + " are not same!");
        }
        if (!config.getTimeUnit().name().equalsIgnoreCase(time.getTimeUnit().name())) {
            throw new IllegalArgumentException("SBK RAM Time Unit: " + config.getTimeUnit().name()
                    + " ,Supplied Time Unit : " + time.getTimeUnit().name() + " are not same!");
        }
        if (config.getMinLatency() != getMinLatency()) {
            Printer.log.warn("SBK RAM , min latency : " + config.getMinLatency()
                    + ", local min latency: " + getMinLatency() + " are not same!");
        }
        if (config.getMaxLatency() != getMaxLatency()) {
            Printer.log.warn("SBK RAM , min latency : " + config.getMaxLatency()
                    + ", local min latency: " + getMaxLatency() + " are not same!");
        }
        try {
            clientID = blockingStub.registerClient(config).getId();
        } catch (StatusRuntimeException ex) {
            ex.printStackTrace();
            throw new IOException("GRPC registerClient failed");
        }

        if (clientID < 0) {
            String errMsg = "Invalid client id: " + clientID + " received from SBK RAM";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        seqNum = 0;
        latencyBytes = 0;
        recorder = new LatencyRecorder(getMinLatency(), getMaxLatency(), LatencyConfig.LONG_MAX,
                LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX);
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
        for (int i = 0; i < maxWriterRequestIds; i++) {
            writeRequestsSum += ramWriteRequestsArray.getAndSet(i, 0);
            writeBytesSum += ramWriteBytesArray.getAndSet(i, 0);
        }
        for (int i = 0; i < maxReaderRequestIds; i++) {
            readRequestsSum += ramReadRequestsArray.getAndSet(i, 0);
            readBytesSum += ramReadBytesArray.getAndSet(i, 0);
        }
        builder.setWriteRequestBytes(writeBytesSum);
        builder.setWriteRequestRecords(writeRequestsSum);
        builder.setReadRequestBytes(readBytesSum);
        builder.setReadRequestRecords(readRequestsSum);
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

    @Override
    public void recordWriteRequests(int writerId, long startTime, long bytes, long events) {
        super.recordWriteRequests(writerId, startTime, bytes, events);
        if (enable) {
            ramWriteRequestsArray.addAndGet(writerId, events);
            ramWriteBytesArray.addAndGet(writerId, bytes);
        }
    }

    @Override
    public void recordReadRequests(int readerId, long startTime, long bytes, long events) {
        super.recordReadRequests(readerId, startTime, bytes, events);
        if (enable) {
            ramReadRequestsArray.addAndGet(readerId, events);
            ramReadBytesArray.addAndGet(readerId, bytes);
        }
    }

    /**
     * record every latency.
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
    public void print(int writers, int maxWriters, int readers, int maxReaders,
                      long writeRequestBytes, double writeRequestsMbPerSec, long writeRequests,
                      double writeRequestsPerSec, long readRequestBytes, double readRequestsMbPerSec,
                      long readRequests, double readRequestsPerSec, double seconds, long bytes,
                      long records, double recsPerSec, double mbPerSec,
                      double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                      long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        super.print(writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestsMbPerSec, writeRequests,
                writeRequestsPerSec, readRequestBytes, readRequestsMbPerSec, readRequests, readRequestsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileValues);
        if (latencyBytes > 0) {
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
