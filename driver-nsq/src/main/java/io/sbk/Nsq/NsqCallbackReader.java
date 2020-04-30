/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Nsq;

import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.DefaultNSQLookup;
import com.github.brainlag.nsq.lookup.NSQLookup;
import io.sbk.api.CallbackReader;
import io.sbk.api.Parameters;
import io.sbk.api.Callback;

import java.io.IOException;

/**
 * Class for Nsq Push Reader.
 */
public class NsqCallbackReader  implements CallbackReader<byte[]> {
    final private String topicName;
    final private String subscriptionName;
    final private  NSQLookup lookup;
    private  NSQConsumer consumer;

    public NsqCallbackReader(int readerId, Parameters params, String topicName,
                                 String subscriptionName, NsqClientConfig config) throws IOException {
        final String[] lookupUri = config.lookupUri.split(":", 2);
        this.topicName = topicName;
        this.subscriptionName = subscriptionName;
        lookup = new DefaultNSQLookup();
        lookup.addLookupAddress(lookupUri[0], Integer.parseInt(lookupUri[1]));
    }

    @Override
    public void start(Callback callback) throws IOException {
        consumer = new NSQConsumer(lookup, topicName, subscriptionName, msg -> {
            //now mark the message as finished.
            callback.consume(msg.getMessage());
            msg.finished();
        });
        consumer.start();
    }

    @Override
    public void close() throws IOException {
        consumer.shutdown();
    }
}