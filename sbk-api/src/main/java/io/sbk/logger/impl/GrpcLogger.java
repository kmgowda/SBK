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


import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
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

/**
 * Streams exact SBK latency frequencies and request counters to an SBM aggregator.
 */
public class GrpcLogger extends SystemLogger {
    private final static String CONFIG_FILE = "sbmhost.properties";
    private final static String NO_HOST_STRING = "none";
    private final static int MAXIMUM_PENDING_BATCHES = 8;

    private SbmHostConfig sbmHostConfig;
    private boolean enable;
    private long clientID;
    private long seqNum;
    private long maxMessageBytes;
    private LatencyRecorder recorder;
    private GrpcLatencyAccumulator latencyAccumulator;
    private ManagedChannel channel;
    private ServiceGrpc.ServiceStub stub;
    private ServiceGrpc.ServiceBlockingStub blockingStub;
    private MessageLatenciesRecord.Builder builder;
    private GrpcStreamSender streamSender;
    private ExceptionHandler exceptionHandler;

    /**
     * Construct a gRPC logger. Calls super to initialize base logging and metrics behavior.
     */
    public GrpcLogger() {
        super();
        this.exceptionHandler = null;
    }

    @Override
    public void setExceptionHandler(ExceptionHandler handler) {
        this.exceptionHandler = handler;
    }

    /**
     * Add SBM host/port options and load defaults from {@code sbmhost.properties}.
     */
    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        super.addArgs(params);
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory());
        try {
            sbmHostConfig = mapper.readValue(
                    GrpcLogger.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                    SbmHostConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }
        maxMessageBytes = (long) sbmHostConfig.maxRecordSizeMB * Bytes.BYTES_PER_MB;
        sbmHostConfig.host = NO_HOST_STRING;
        params.addOption("sbm", true, "SBM host" +
                "; '" + NO_HOST_STRING + "' disables this option, default: " + sbmHostConfig.host);
        params.addOption("sbmport", true, "SBM Port" +
                "; default: " + sbmHostConfig.port);
        //params.addOption("blocking", true, "blocking calls to SBM; default: false");
    }

    /**
     * Parse SBM options and decide if gRPC export is enabled.
     */
    @Override
    public void parseArgs(final ParsedOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        sbmHostConfig.host = params.getOptionValue("sbm", sbmHostConfig.host);
        enable = !sbmHostConfig.host.equalsIgnoreCase(NO_HOST_STRING);
        if (!enable) {
            return;
        }
        sbmHostConfig.port = Integer.parseInt(params.getOptionValue("sbmport", Integer.toString(sbmHostConfig.port)));
    }

    /**
     * Open the logger, establish a gRPC channel, validate configuration with SBM, and prepare buffers.
     */
    @Override
    public void open(final ParsedOptions params, final String storageName, Action action, Time time) throws IllegalArgumentException, IOException {
        super.open(params, storageName, action, time);
        if (!enable) {
            return;
        }
        channel = ManagedChannelBuilder.forTarget(sbmHostConfig.host + ":" + sbmHostConfig.port).usePlaintext().build();
        blockingStub = ServiceGrpc.newBlockingStub(channel);
        Version sbmSbpVersion;
        try {
            sbmSbpVersion = blockingStub.getVersion(Empty.getDefaultInstance());
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
            config = blockingStub.getConfig(Empty.getDefaultInstance());
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
        if (config.getMaxRecordSizeBytes() > 0) {
            maxMessageBytes = Math.min(maxMessageBytes, config.getMaxRecordSizeBytes());
        }
        Printer.log.info("SBK GRPC Logger maximum latency-record size: " + maxMessageBytes + " bytes");

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
        recorder = new LatencyRecorder(getMinLatency(), getMaxLatency(), LatencyConfig.LONG_MAX,
                LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX);
        latencyAccumulator = new GrpcLatencyAccumulator(maxMessageBytes);
        builder = MessageLatenciesRecord.newBuilder();
        stub = ServiceGrpc.newStub(channel);
        streamSender = new GrpcStreamSender(stub, MAXIMUM_PENDING_BATCHES, this::reportTransportFailure);
        Printer.log.info("SBK GRPC Logger transport: SBP client stream with packed primitive latencies");
        Printer.log.info("SBK GRPC Logger Started");
    }

    /**
     * Close the logger, unregister the client, and shutdown the gRPC channel.
     */
    @Override
    public void close(final ParsedOptions params) throws IllegalArgumentException, IOException {
        super.close(params);
        if (!enable) {
            return;
        }
        IOException failure = null;
        try {
            if (streamSender != null) {
                streamSender.close();
            }
        } catch (IOException exception) {
            failure = exception;
        }
        try {
            blockingStub.closeClient(ClientID.newBuilder().setId(clientID).build());
        } catch (StatusRuntimeException exception) {
            if (failure == null) {
                failure = new IOException("GRPC closeClient failed", exception);
            } else {
                failure.addSuppressed(exception);
            }
        }
        channel.shutdown();
        try {
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
                channel.awaitTermination(1, TimeUnit.SECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            channel.shutdownNow();
            if (failure == null) {
                failure = new IOException("Interrupted while closing the gRPC channel", exception);
            } else {
                failure.addSuppressed(exception);
            }
        }
        builder.clear();
        latencyAccumulator.clear();
        Printer.log.info("SBK GRPC Logger Shutdown");
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Send the accumulated request/latency counters to SBM and reset local accumulators.
     *
     * @param writeRequestBytes write-request bytes represented by this batch
     * @param writeRequestRecords write-request records represented by this batch
     * @param readRequestBytes read-request bytes represented by this batch
     * @param readRequestRecords read-request records represented by this batch
     * @param writeTimeoutEvents write timeout events represented by this batch
     * @param readTimeoutEvents read timeout events represented by this batch
     */
    private void sendLatenciesRecord(long writeRequestBytes, long writeRequestRecords,
                                     long readRequestBytes, long readRequestRecords,
                                     long writeTimeoutEvents, long readTimeoutEvents) {
        builder.setWriteRequestBytes(writeRequestBytes);
        builder.setWriteRequestRecords(writeRequestRecords);
        builder.setReadRequestBytes(readRequestBytes);
        builder.setReadRequestRecords(readRequestRecords);
        builder.setWriteTimeoutEvents(writeTimeoutEvents);
        builder.setReadTimeoutEvents(readTimeoutEvents);
        builder.setClientID(clientID);
        builder.setSequenceNumber(++seqNum);
        builder.setMaxReaders(getMaxReadersCount());
        builder.setReaders(getReadersCount());
        builder.setWriters(getWritersCount());
        builder.setMaxWriters(getMaxWritersCount());
        builder.setMinLatency(recorder.getMinLatency());
        builder.setMaxLatency(recorder.getMaxLatency());
        builder.setTotalLatency(recorder.getTotalLatency());
        builder.setInvalidLatencyRecords(recorder.getInvalidLatencyRecords());
        builder.setTotalBytes(recorder.getTotalBytes());
        builder.setTotalRecords(recorder.getTotalRecords());
        builder.setHigherLatencyDiscardRecords(recorder.getHigherLatencyDiscardRecords());
        builder.setLowerLatencyDiscardRecords(recorder.getLowerLatencyDiscardRecords());
        builder.setValidLatencyRecords(recorder.getValidLatencyRecords());

        latencyAccumulator.writePacked(builder);
        final MessageLatenciesRecord record = builder.build();
        if (record.getSerializedSize() > maxMessageBytes) {
            reportTransportFailure(new IOException("SBK gRPC latency record size " + record.getSerializedSize()
                    + " exceeds configured maximum " + maxMessageBytes + " bytes"));
        } else {
            try {
                streamSender.send(record);
            } catch (IOException exception) {
                reportTransportFailure(exception);
            }
        }
        recorder.reset();
        builder.clear();
        latencyAccumulator.clear();
    }

    /**
     * Record individual latency values into the local {@code LatencyRecorder} and stage them for gRPC export.
     */
    @Override
    public void recordLatency(long startTime, int events, int bytes, long latency) {
        if (!enable) {
            return;
        }

        if (latencyAccumulator.isFull()) {
            sendLatenciesRecord(0, 0, 0, 0, 0, 0);
        }
        if (recorder.record(events, bytes, latency)) {
            latencyAccumulator.record(latency, events);
        }
    }

    @Override
    public void print(long reportTime, int writers, int maxWriters, int readers, int maxReaders,
                      long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                      double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                      long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
                      long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                      long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                      long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                      long readTimeoutEvents, double readTimeoutEventsPerSec,
                      double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                      double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                      long higherDiscard, long slc1, long slc2, long[] percentileLatencies,
                      long[] percentileLatencyCounts) {
        super.print(reportTime, writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestMbPerSec,
                writeRequestRecords, writeRequestRecordsPerSec, readRequestBytes, readRequestMbPerSec,
                readRequestRecords, readRequestRecordsPerSec, writeResponsePendingRecords, writeResponsePendingBytes,
                readResponsePendingRecords, readResponsePendingBytes, writeReadRequestPendingRecords,
                writeReadRequestPendingBytes, writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents,
                readTimeoutEventsPerSec, seconds, bytes, records, recsPerSec, mbPerSec, avgLatency,
                minLatency, maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileLatencies,
                percentileLatencyCounts);
        if (enable) {
            sendLatenciesRecord(writeRequestBytes, writeRequestRecords,
                    readRequestBytes, readRequestRecords, writeTimeoutEvents, readTimeoutEvents);
        }
    }

    private void reportTransportFailure(Throwable throwable) {
        if (exceptionHandler != null) {
            exceptionHandler.throwException(throwable);
        } else {
            Printer.log.error("SBK gRPC logger transport failure", throwable);
        }
    }

}
