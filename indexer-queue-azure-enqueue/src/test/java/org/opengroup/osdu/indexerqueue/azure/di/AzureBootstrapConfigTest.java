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
    void shouldReturnSetValue_when_getKeyVaultURL_isCalled() {
        assertEquals(keyVaultURL, sut.keyVaultURL());
    }

    @Test
    void shouldReturnNotNullValue_when_getMdcContextMap_isCalled() {
        assertNotNull(sut.mdcContextMap());
    }

    @Test
    void shouldReturnSetValue_when_getAppResourceId_isCalled() {
        assertEquals(sut.getAppResourceId(), appResourceId);
    }

    @Test
    void shouldReturnSetValue_when_getNThreads_isCalled() {
        assertEquals(sut.getNThreads(), nThreads);
    }

    @Test
    void shouldReturnSetValue_when_getMaxConcurrentCalls_isCalled() {
        assertEquals(sut.getMaxConcurrentCalls(), maxConcurrentCalls);
    }

    @Test
    void shouldReturnSetValue_when_getMaxLockRenewDurationInSeconds_isCalled() {
        assertEquals(sut.getMaxLockRenewDurationInSeconds(), maxLockRenewDurationInSeconds);
    }

    @Test
    void shouldReturnSetValue_when_getMaxDeliveryCount_isCalled() {
        assertEquals(sut.getMaxDeliveryCount(), maxDeliveryCount);
    }

    @Test
    void shouldReturnSetValue_when_getServiceBusTopic_isCalled() {
        assertEquals(sut.getServiceBusTopic(), serviceBusTopic);
    }

    @Test
    void shouldReturnSetValue_when_getServiceBusTopicSubscription_isCalled() {
        assertEquals(sut.getServiceBusTopicSubscription(), serviceBusTopicSubscription);
    }

    @Test
    void shouldReturnSetValue_when_getIndexerWorkerURL_isCalled() {
        assertEquals(sut.getIndexerWorkerURL(), indexerWorkerURL);
    }

    @Test
    void shouldReturnSetValue_when_getSleepDurationForMainThreadInSeconds_isCalled() {
        assertEquals(sut.getSleepDurationForMainThreadInSeconds(), sleepDurationForMainThreadInSeconds);
    }

    @Test
    void shouldReturnSetValue_when_getAppName_isCalled() {
        assertEquals(sut.getAppName(), appName);
    }
}
