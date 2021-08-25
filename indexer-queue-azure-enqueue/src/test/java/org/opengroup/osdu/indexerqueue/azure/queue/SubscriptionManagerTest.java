package org.opengroup.osdu.indexerqueue.azure.queue;

import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.indexerqueue.azure.di.AzureBootstrapConfig;
import org.opengroup.osdu.indexerqueue.azure.util.SbMessageBuilder;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SubscriptionManagerTest {

    private static final String maxLockRenewDuration = "60";
    private static final String maxConcurrentCalls = "1";
    private static final String nThreads = "2";
    private static final String errorMessage = "some-error";

    @InjectMocks
    private SubscriptionManager sut;
    @Mock
    private SbMessageBuilder sbMessageBuilder;
    @Mock
    private RecordChangedMessageHandler recordChangedMessageHandler;
    @Mock
    private AzureBootstrapConfig azureBootstrapConfig;
    @Mock
    private ITenantFactory tenantFactory;
    @Mock
    private SubscriptionClientFactory clientFactory;
    @Mock
    private SubscriptionClient subscriptionClient;

    private static final String dataPartition = "testTenant";

    @BeforeEach
    public void init() {
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setDataPartitionId(dataPartition);

        when(azureBootstrapConfig.getMaxConcurrentCalls()).thenReturn(maxConcurrentCalls);
        when(azureBootstrapConfig.getNThreads()).thenReturn(nThreads);
        when(azureBootstrapConfig.getMaxLockRenewDurationInSeconds()).thenReturn(maxLockRenewDuration);
        when(tenantFactory.listTenantInfo()).thenReturn(Collections.singletonList(tenantInfo));
    }

    @Test
    public void shouldSuccessfullyRegisterMessageHandler() throws ServiceBusException, InterruptedException {

        doNothing().when(subscriptionClient).registerMessageHandler(any(), any(), any());
        when(clientFactory.getSubscriptionClient(dataPartition)).thenReturn(subscriptionClient);

        sut.subscribeRecordsTopic();

        verify(azureBootstrapConfig, times(1)).getMaxConcurrentCalls();
        verify(azureBootstrapConfig, times(1)).getNThreads();
        verify(azureBootstrapConfig, times(1)).getMaxLockRenewDurationInSeconds();
    }

    @Test
    public void shouldCatchExceptionIfErrorWhileRegisteringMessageHandler() throws ServiceBusException, InterruptedException {

        doThrow(new InterruptedException(errorMessage)).when(subscriptionClient).registerMessageHandler(any(), any(), any());
        when(clientFactory.getSubscriptionClient(dataPartition)).thenReturn(subscriptionClient);

        sut.subscribeRecordsTopic();

        verify(azureBootstrapConfig, times(1)).getMaxConcurrentCalls();
        verify(azureBootstrapConfig, times(1)).getNThreads();
        verify(azureBootstrapConfig, times(1)).getMaxLockRenewDurationInSeconds();
    }
}
