package org.opengroup.osdu.indexerqueue.azure.scope.thread;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class ThreadScopeBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
        factory.registerScope("ThreadScope", new ThreadScope());
    }
}