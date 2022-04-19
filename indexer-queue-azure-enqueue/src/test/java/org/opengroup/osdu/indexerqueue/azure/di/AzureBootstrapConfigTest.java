package org.opengroup.osdu.indexerqueue.azure.di;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.jupiter.api.Assertions.*;

class AzureBootstrapConfigTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(AzureBootstrapConfig.class);

    @Value("${azure.keyvault.url}")
    private String keyVaultURL;

    @Value("${azure.app.resource.id}")
    private String appResourceId;

    @Value("${executor-n-threads}")
    private String nThreads;

    @Value("${max-concurrent-calls}")
    private String maxConcurrentCalls;

    @Value("${max-lock-renew-duration-seconds}")
    private String maxLockRenewDurationInSeconds;

    @Value("${max-delivery-count}")
    private String maxDeliveryCount;

    @Value("${azure.servicebus.topic-name}")
    private String serviceBusTopic;

    @Value("${azure.servicebus.topic-subscription}")
    private String serviceBusTopicSubscription;

    @Value("${indexer.worker.url}")
    private String indexerWorkerURL;

    @Value("#{new Integer('${sleep.duration.main.thread.seconds}')}")
    private Integer sleepDurationForMainThreadInSeconds;

    @Value("${spring.application.name}")
    private String appName;

    @InjectMocks
    AzureBootstrapConfig sut = new AzureBootstrapConfig();

    @Test
    void should_getKeyVaultURL() {
        assertEquals(keyVaultURL, sut.keyVaultURL());
    }

    @Test
    void should_getMdcContextMap() {
        assertNotNull(sut.mdcContextMap());
    }

    @Test
    void should_getAppResourceId() {
        assertEquals(sut.getAppResourceId(), appResourceId);
    }

    @Test
    void should_getNThreads() {
        assertEquals(sut.getNThreads(), nThreads);
    }

    @Test
    void should_getMaxConcurrentCalls() {
        assertEquals(sut.getMaxConcurrentCalls(), maxConcurrentCalls);
    }

    @Test
    void should_getMaxLockRenewDurationInSeconds() {
        assertEquals(sut.getMaxLockRenewDurationInSeconds(), maxLockRenewDurationInSeconds);
    }

    @Test
    void should_getMaxDeliveryCount() {
        assertEquals(sut.getMaxDeliveryCount(), maxDeliveryCount);
    }

    @Test
    void should_getServiceBusTopic() {
        assertEquals(sut.getServiceBusTopic(), serviceBusTopic);
    }

    @Test
    void should_getServiceBusTopicSubscription() {
        assertEquals(sut.getServiceBusTopicSubscription(), serviceBusTopicSubscription);
    }

    @Test
    void should_getIndexerWorkerURL() {
        assertEquals(sut.getIndexerWorkerURL(), indexerWorkerURL);
    }

    @Test
    void should_getSleepDurationForMainThreadInSeconds() {
        assertEquals(sut.getSleepDurationForMainThreadInSeconds(), sleepDurationForMainThreadInSeconds);
    }

    @Test
    void should_getAppName() {
        assertEquals(sut.getAppName(), appName);
    }
}