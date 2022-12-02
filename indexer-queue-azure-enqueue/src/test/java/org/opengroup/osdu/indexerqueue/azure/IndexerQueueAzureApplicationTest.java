package org.opengroup.osdu.indexerqueue.azure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IndexerQueueAzureApplicationTest {

    @InjectMocks
    IndexerQueueAzureApplication sut=new IndexerQueueAzureApplication();

    @Test
    void should_returnThreadScopeBeanFactoryPostProcessor_whenBeanFactoryPostProcessorCalled() {
        assertNotNull(sut.beanFactoryPostProcessor());
    }
}
