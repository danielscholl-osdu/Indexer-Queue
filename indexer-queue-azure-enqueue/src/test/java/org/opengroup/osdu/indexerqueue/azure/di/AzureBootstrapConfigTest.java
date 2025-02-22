package org.opengroup.osdu.indexerqueue.azure.di;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    @Value("schema.worker.url")
    private String schemaWorkerURL;

    @Mock
    @Value("100")
    private Integer sleepDurationForMainThreadInSeconds;

    @Mock
    @Value("IndexerQueue")
    private String appName;

    @Mock
    @Value("azure.reindex.topic-name")
    private String reindexTopic;

    @Mock
    @Value("azure.reindex.topic-subscription")
    private String reindexTopicSubscription;

    @Mock
    @Value("azure.schemachanged.topic-name")
    private String schemachangedTopic;

    @Mock
    @Value("azure.schemachanged.topic-subscription")
    private String schemachangedSubscription;

    @InjectMocks
    AzureBootstrapConfig sut = new AzureBootstrapConfig();

    @Test
    void shouldReturnSetValue_when_getKeyVaultURL_isCalled() {
        assertEquals(keyVaultURL, sut.getKeyVaultURL());
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
    void shouldReturnSetValue_when_getSchemaWorkerURL_isCalled() {
        assertEquals(sut.getSchemaWorkerURL(), schemaWorkerURL);
    }

    @Test
    void shouldReturnSetValue_when_getSleepDurationForMainThreadInSeconds_isCalled() {
        assertEquals(sut.getSleepDurationForMainThreadInSeconds(), sleepDurationForMainThreadInSeconds);
    }

    @Test
    void shouldReturnSetValue_when_getAppName_isCalled() {
        assertEquals(sut.getAppName(), appName);
    }

    @Test
    void shouldReturnSetValue_when_getReindexTopic_isCalled() {
        assertEquals(sut.getReindexTopic(), reindexTopic);
    }

    @Test
    void shouldReturnSetValue_when_getReindexTopicSubscription_isCalled() {
        assertEquals(sut.getReindexTopicSubscription(), reindexTopicSubscription);
    }

    @Test
    void shouldReturnSetValue_when_getSchemachangedTopic_isCalled() {
        assertEquals(sut.getSchemachangedTopic(), schemachangedTopic);
    }

    @Test
    void shouldReturnSetValue_when_getSchemachangedSubscription_isCalled() {
        assertEquals(sut.getSchemachangedSubscription(), schemachangedSubscription);
    }

}
