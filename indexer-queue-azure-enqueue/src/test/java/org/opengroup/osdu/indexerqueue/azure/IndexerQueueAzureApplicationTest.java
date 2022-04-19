package org.opengroup.osdu.indexerqueue.azure;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.junit.jupiter.api.Assertions.*;

class IndexerQueueAzureApplicationTest {

    @InjectMocks
    IndexerQueueAzureApplication sut=new IndexerQueueAzureApplication();

    @Test
    void should_returnThreadScopeBeanFactoryPostProcessor_whenBeanFactoryPostProcessorCalled() {
        assertNotNull(sut.beanFactoryPostProcessor());
    }
}