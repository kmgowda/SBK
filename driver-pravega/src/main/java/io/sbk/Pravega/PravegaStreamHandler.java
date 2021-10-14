/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Pravega;

import io.pravega.client.ClientConfig;
import io.pravega.client.admin.ReaderGroupManager;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.segment.impl.Segment;
import io.pravega.client.stream.ReaderGroup;
import io.pravega.client.stream.ReaderGroupConfig;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.Stream;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.control.impl.ControllerImpl;
import io.pravega.client.stream.impl.StreamImpl;
import io.pravega.client.stream.impl.StreamSegments;
import io.sbk.system.Printer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class for Pravega stream and segments.
 */
public class PravegaStreamHandler {
    final String scope;
    final String stream;
    final String rdGrpName;
    final String controllerUri;
    final ControllerImpl controller;
    final StreamManager streamManager;
    final StreamConfiguration streamconfig;
    final ScheduledExecutorService bgexecutor;
    final int segCount;
    final int timeout;
    ReaderGroupManager readerGroupManager;
    ReaderGroupConfig rdGrpConfig;

    public PravegaStreamHandler(String scope, String stream,
                                String rdGrpName,
                                String uri, int segs,
                                int timeout, ControllerImpl contrl,
                                ScheduledExecutorService bgexecutor) throws Exception {
        this.scope = scope;
        this.stream = stream;
        this.rdGrpName = rdGrpName;
        this.controllerUri = uri;
        this.controller = contrl;
        this.segCount = segs;
        this.timeout = timeout;
        this.bgexecutor = bgexecutor;
        streamManager = StreamManager.create(new URI(uri));
        streamManager.createScope(scope);
        streamconfig = StreamConfiguration.builder().scalingPolicy(ScalingPolicy.fixed(segCount)).build();
    }

    public boolean create() {
        return streamManager.createStream(scope, stream, streamconfig);
    }

    public void scale() throws InterruptedException, ExecutionException, TimeoutException {
        StreamSegments segments = controller.getCurrentSegments(scope, stream).join();
        final int nseg = segments.getSegments().size();
        Printer.log.info("Current segments of the stream: " + stream + " = " + nseg);

        if (nseg == segCount) {
            return;
        }

        Printer.log.info("The stream: " + stream + " will be manually scaling to " + segCount + " segments");

        /*
         * Note that the Upgrade stream API does not change the number of segments;
         * but it indicates with new number of segments.
         * after calling update stream , manual scaling is required
         */
        if (!streamManager.updateStream(scope, stream, streamconfig)) {
            throw new TimeoutException("Could not able to update the stream: " + stream + " try with another stream Name");
        }

        final double keyRangeChunk = 1.0 / segCount;
        final Map<Double, Double> keyRanges = IntStream.range(0, segCount)
                .boxed()
                .collect(
                        Collectors
                                .toMap(x -> x * keyRangeChunk,
                                        x -> (x + 1) * keyRangeChunk));
        final List<Long> segmentList = segments.getSegments()
                .stream()
                .map(Segment::getSegmentId)
                .collect(Collectors.toList());

        CompletableFuture<Boolean> scaleStatus = controller.scaleStream(new StreamImpl(scope, stream),
                segmentList,
                keyRanges,
                bgexecutor).getFuture();

        if (!scaleStatus.get(timeout, TimeUnit.SECONDS)) {
            throw new TimeoutException("ERROR : Scale operation on stream " + stream + " did not complete");
        }

        Printer.log.info("Number of Segments after manual scale: " +
                controller.getCurrentSegments(scope, stream)
                        .get().getSegments().size());
    }

    public void recreate() throws InterruptedException, ExecutionException, TimeoutException {
        Printer.log.info("Sealing and Deleteing the stream : " + stream + " and then recreating the same");
        CompletableFuture<Boolean> sealStatus = controller.sealStream(scope, stream);
        if (!sealStatus.get(timeout, TimeUnit.SECONDS)) {
            throw new TimeoutException("ERROR : Segment sealing operation on stream " + stream + " did not complete");
        }

        CompletableFuture<Boolean> status = controller.deleteStream(scope, stream);
        if (!status.get(timeout, TimeUnit.SECONDS)) {
            throw new TimeoutException("ERROR : stream: " + stream + " delete failed");
        }

        if (!streamManager.createStream(scope, stream, streamconfig)) {
            throw new TimeoutException("ERROR : stream: " + stream + " recreation failed");
        }
    }

    public ReaderGroup createReaderGroup(boolean reset) throws URISyntaxException {
        if (readerGroupManager == null) {
            readerGroupManager = ReaderGroupManager.withScope(scope,
                    ClientConfig.builder().controllerURI(new URI(controllerUri)).build());
            rdGrpConfig = ReaderGroupConfig.builder()
                    .stream(Stream.of(scope, stream)).build();
        }
        readerGroupManager.createReaderGroup(rdGrpName, rdGrpConfig);
        final ReaderGroup rdGroup = readerGroupManager.getReaderGroup(rdGrpName);
        if (reset) {
            rdGroup.resetReaderGroup(rdGrpConfig);
        }
        return rdGroup;
    }

    public void deleteReaderGroup() {
        try {
            readerGroupManager.deleteReaderGroup(rdGrpName);
        } catch (RuntimeException e) {
            Printer.log.error("Cannot delete reader group " + rdGrpName + " because it is already deleted");
        }
    }
}
