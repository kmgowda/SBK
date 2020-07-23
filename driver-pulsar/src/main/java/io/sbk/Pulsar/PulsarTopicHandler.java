/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Pulsar;

import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.admin.PulsarAdminException.ConflictException;
import org.apache.pulsar.client.admin.PulsarAdminException.NotFoundException;
import org.apache.pulsar.common.policies.data.BacklogQuota;
import org.apache.pulsar.common.policies.data.BacklogQuota.RetentionPolicy;
import org.apache.pulsar.common.policies.data.PersistencePolicies;
import org.apache.pulsar.common.policies.data.TenantInfo;
import org.apache.pulsar.common.policies.data.ClusterData;
import com.google.common.collect.Sets;


import java.io.IOException;
import java.util.Collections;

/**
 * Class for Pulsar topic and partitions.
 */
public class PulsarTopicHandler {
    final private PulsarConfig config;
    final private PulsarAdmin adminClient;
    final private PulsarAdminBuilder pulsarAdmin;


    public PulsarTopicHandler(PulsarConfig config) throws IOException {
        this.config = config;
        try {
            pulsarAdmin = PulsarAdmin.builder().serviceHttpUrl(config.adminUri);
            adminClient = pulsarAdmin.build();
        } catch (PulsarClientException ex) {
            ex.printStackTrace();
            throw new IOException(ex);
        }
    }


    public void createTopic(boolean recreate) throws  IOException {
        if (config.tenant != null && config.nameSpace != null) {
            final String fullNameSpace = config.tenant + "/" + config.nameSpace;
            if (config.cluster != null) {
                try {
                    ClusterData clusterData = new ClusterData(
                            config.adminUri,
                            null,
                            config.brokerUri,
                            null
                    );
                    adminClient.clusters().createCluster(config.cluster, clusterData);

                    if (!adminClient.tenants().getTenants().contains(config.tenant)) {
                        adminClient.tenants().createTenant(config.tenant,
                                new TenantInfo(Collections.emptySet(), Sets.newHashSet(config.cluster)));
                    }
                } catch (ConflictException ex) {
                    ex.printStackTrace();
                } catch (PulsarAdminException ex) {
                    throw new IOException(ex);
                }
            }
            try {
                adminClient.namespaces().createNamespace(fullNameSpace);
            } catch (ConflictException ex) {
               /* ex.printStackTrace(); */
            } catch (PulsarAdminException ex) {
                throw new IOException(ex);
            }

            try {
                adminClient.namespaces().setPersistence(fullNameSpace,
                        new PersistencePolicies(config.ensembleSize, config.writeQuorum,
                                config.ackQuorum, 1.0));
                adminClient.namespaces().setBacklogQuota(fullNameSpace,
                        new BacklogQuota(Long.MAX_VALUE, RetentionPolicy.producer_exception));
                adminClient.namespaces().setDeduplicationStatus(fullNameSpace, config.deduplicationEnabled);

            } catch (PulsarAdminException ex) {
                throw new IOException(ex);
            }
        }
        if (recreate) {
            try {
                adminClient.topics().deletePartitionedTopic(config.topicName);
            } catch (NotFoundException  ex) {
                /* already deleted or not existing */
            } catch (PulsarAdminException ex) {
                throw new IOException(ex);
            }
        }

        try {
            adminClient.topics().createPartitionedTopic(config.topicName, config.partitions);
        } catch (ConflictException ex) {
            /* ex.printStackTrace(); */
        } catch (PulsarAdminException ex) {
            throw new IOException(ex);
        }
    }

    public  void close() {
        pulsarAdmin.clone();
    }
}
