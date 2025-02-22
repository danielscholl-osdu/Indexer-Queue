package org.opengroup.osdu.indexerqueue.azure;

import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.opengroup.osdu.indexerqueue.azure.queue.SubscriptionManager;
import org.opengroup.osdu.indexerqueue.azure.scope.thread.ThreadScopeBeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(value = {
        "org.opengroup.osdu"
})
public class IndexerQueueAzureApplication {
    public static void main(String[] args) throws ServiceBusException, InterruptedException {
        ApplicationContext context = SpringApplication.run(IndexerQueueAzureApplication.class, args);

        SubscriptionManager subscriptionManager = context.getBean(SubscriptionManager.class);
        subscriptionManager.subscribeRecordsTopic();
    }

    @Bean
    public static BeanFactoryPostProcessor beanFactoryPostProcessor() {
        return new ThreadScopeBeanFactoryPostProcessor();
    }
}