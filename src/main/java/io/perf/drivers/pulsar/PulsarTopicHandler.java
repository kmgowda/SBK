package io.perf.drivers.Pulsar;

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

public class PulsarTopicHandler {
    final private PulsarAdmin adminClient;
    final private PulsarAdminBuilder pulsarAdmin;
    final private String nameSpace;
    final private String cluster;
    final private String tenant;
    final private String adminUri;
    final private String brokerUri;
    final private String topicName;
    final private int partitions;
    final private int ensembleSize;
    final private int writeQuorum;
    final private int ackQuorum;
    final private boolean deduplicationEnabled;

    public PulsarTopicHandler(String adminUri, String brokerUri, String tenant, String cluster,
                              String nameSpace, String topicName, int partitions,
                              int ensembleSize, int writeQuorum, int ackQuorum, boolean deduplicationEnabled) throws IOException {
        this.adminUri = adminUri;
        this.brokerUri = brokerUri;
        this.tenant = tenant;
        this.cluster = cluster;
        this.nameSpace = nameSpace;
        this.topicName = topicName;
        this.partitions = partitions;
        this.ensembleSize = ensembleSize;
        this.writeQuorum = writeQuorum;
        this.ackQuorum = ackQuorum;
        this.deduplicationEnabled = deduplicationEnabled;
        try {
            pulsarAdmin = PulsarAdmin.builder().serviceHttpUrl(adminUri);
            adminClient = pulsarAdmin.build();
        } catch (PulsarClientException ex) {
            ex.printStackTrace();
            throw new IOException(ex);
        }
    }


    public void createTopic(boolean recreate) throws  IOException {
        if (tenant != null && nameSpace != null) {
            final String fullNameSpace = tenant + "/" + nameSpace;
            if (cluster != null) {
                try {
                    ClusterData clusterData = new ClusterData(
                            adminUri,
                            null,
                            brokerUri,
                            null
                    );
                    adminClient.clusters().createCluster(cluster, clusterData);

                    if (!adminClient.tenants().getTenants().contains(tenant)) {
                        adminClient.tenants().createTenant(tenant,
                                new TenantInfo(Collections.emptySet(), Sets.newHashSet(cluster)));
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
                        new PersistencePolicies(ensembleSize, writeQuorum, ackQuorum, 1.0));

                adminClient.namespaces().setBacklogQuota(fullNameSpace,
                        new BacklogQuota(Long.MAX_VALUE, RetentionPolicy.producer_exception));
                adminClient.namespaces().setDeduplicationStatus(fullNameSpace, deduplicationEnabled);

            } catch (PulsarAdminException ex) {
                throw new IOException(ex);
            }
        }
        if (recreate) {
            try {
                adminClient.topics().deletePartitionedTopic(topicName);
            } catch (NotFoundException  ex) {
                /* already deleted or not existing */
            } catch (PulsarAdminException ex) {
                throw new IOException(ex);
            }
        }

        try {
            adminClient.topics().createPartitionedTopic(topicName, partitions);
        } catch (ConflictException ex) {
            /* ex.printStackTrace(); */
        } catch (PulsarAdminException ex) {
            throw new IOException(ex);
        }
    }


}
