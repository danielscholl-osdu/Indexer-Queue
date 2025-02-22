package org.opengroup.osdu.indexerqueue.azure.scope.thread;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import static org.junit.jupiter.api.Assertions.*;

class ThreadScopeBeanFactoryPostProcessorTest {

    @Mock
    ConfigurableListableBeanFactory configurableListableBeanFactory=new DefaultListableBeanFactory();
    @InjectMocks
    ThreadScopeBeanFactoryPostProcessor sut=new  ThreadScopeBeanFactoryPostProcessor();

    @Test
    void should_returnNotNull_whenPostProcessBeanFactorySetScope() {
        sut.postProcessBeanFactory(configurableListableBeanFactory);
        assertNotNull(configurableListableBeanFactory.getRegisteredScope("ThreadScope"));
    }
}
