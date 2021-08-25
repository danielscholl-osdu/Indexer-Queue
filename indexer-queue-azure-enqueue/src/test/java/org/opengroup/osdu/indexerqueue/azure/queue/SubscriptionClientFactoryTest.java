package org.opengroup.osdu.indexerqueue.azure.queue;

import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.servicebus.ISubscriptionClientFactory;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.indexerqueue.azure.di.AzureBootstrapConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SubscriptionClientFactoryTest {
    @InjectMocks
    private SubscriptionClientFactory sut;

    @Mock
    private SubscriptionClient subscriptionClient;

    @Mock
    private ISubscriptionClientFactory subscriptionClientFactory;

    @Mock
    private AzureBootstrapConfig azureBootstrapConfig;

    private static final String sbTopic = "testTopic";
    private static final String sbSubscription = "testSubscription";
    private static final String dataPartition = "testPartition";

    @Test
    public void subscriptionClientShouldNotBeNull() throws ServiceBusException, InterruptedException {
        when(azureBootstrapConfig.getServiceBusTopic()).thenReturn(sbTopic);
        when(azureBootstrapConfig.getServiceBusTopicSubscription()).thenReturn(sbSubscription);
        when(subscriptionClientFactory.getClient(dataPartition, sbTopic, sbSubscription))
                .thenReturn(subscriptionClient);

        SubscriptionClient result = sut.getSubscriptionClient(dataPartition);
        assertNotNull(result);
        assertEquals(subscriptionClient, result);
    }

    @Test
    public void shouldThrow_when_subscriptionClientFactory_ThrowsInterruptedException() throws ServiceBusException, InterruptedException {
        when(azureBootstrapConfig.getServiceBusTopic()).thenReturn(sbTopic);
        when(azureBootstrapConfig.getServiceBusTopicSubscription()).thenReturn(sbSubscription);
        doThrow(new InterruptedException()).when(subscriptionClientFactory).getClient(dataPartition, sbTopic, sbSubscription);

        try {
            SubscriptionClient result = sut.getSubscriptionClient(dataPartition);
        } catch (Exception e) {
            assertEquals(e.getClass(), AppException.class);
            assertEquals(e.getMessage(), "Unexpected error creating Subscription Client");
        }
    }

    @Test
    public void shouldThrow_when_subscriptionClientFactory_ThrowsServiceBusException() throws ServiceBusException, InterruptedException {
        when(azureBootstrapConfig.getServiceBusTopic()).thenReturn(sbTopic);
        when(azureBootstrapConfig.getServiceBusTopicSubscription()).thenReturn(sbSubscription);
        doThrow(new ServiceBusException(true)).when(subscriptionClientFactory).getClient(dataPartition, sbTopic, sbSubscription);

        try {
            SubscriptionClient result = sut.getSubscriptionClient(dataPartition);
        } catch (Exception e) {
            assertEquals(e.getClass(), AppException.class);
            assertEquals(e.getMessage(), "Unexpected error creating Subscription Client");
        }
    }
}
