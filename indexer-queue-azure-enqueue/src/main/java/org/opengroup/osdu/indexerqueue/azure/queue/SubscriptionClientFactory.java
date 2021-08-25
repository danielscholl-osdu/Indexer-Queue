package org.opengroup.osdu.indexerqueue.azure.queue;

import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.opengroup.osdu.azure.servicebus.ISubscriptionClientFactory;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.indexerqueue.azure.di.AzureBootstrapConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionClientFactory {

    @Autowired
    private AzureBootstrapConfig azureBootstrapConfig;

    @Autowired private ISubscriptionClientFactory subscriptionClientFactory;

    public SubscriptionClient getSubscriptionClient(String dataPartition) {
        String sbTopic = azureBootstrapConfig.getServiceBusTopic();
        String sbSubscription = azureBootstrapConfig.getServiceBusTopicSubscription();
        try {
            return subscriptionClientFactory.getClient(dataPartition, sbTopic, sbSubscription);
        } catch (ServiceBusException | InterruptedException e) {
            throw new AppException(500, "Server Error", "Unexpected error creating Subscription Client", e);
        }
    }
}
