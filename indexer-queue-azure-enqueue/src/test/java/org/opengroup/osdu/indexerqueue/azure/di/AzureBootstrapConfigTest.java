package org.opengroup.osdu.indexerqueue.azure.di;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import static org.junit.jupiter.api.Assertions.*;

class AzureBootstrapConfigTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(AzureBootstrapConfig.class);

    @Mock
    @Value("keyVaultURLVal")
    private String keyVaultURL;

    @Mock
    @Value("appResourceIdVal")
    private String appResourceId;

    @Mock
    @Value("nThreads")
    private String nThreads;

    @Mock
    @Value("maxConcurrentCalls")
    private String maxConcurrentCalls;

    @Mock
    @Value("max-lock-renew-duration-seconds")
    private String maxLockRenewDurationInSeconds;

    @Mock
    @Value("max-delivery-count")
    private String maxDeliveryCount;

    @Mock
    @Value("azure.servicebus.topic-name")
    private String serviceBusTopic;

    @Mock
    @Value("azure.servicebus.topic-subscription")
    private String serviceBusTopicSubscription;

    @Mock
    @Value("indexer.worker.url")
    private String indexerWorkerURL;

    @Mock
    @Value("100")
    private Integer sleepDurationForMainThreadInSeconds;

    @Mock
    @Value("IndexerQueue")
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
