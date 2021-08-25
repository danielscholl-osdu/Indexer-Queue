package org.opengroup.osdu.indexerqueue.azure.queue;

import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.ICoreLogger;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.indexerqueue.azure.di.AzureBootstrapConfig;
import org.opengroup.osdu.indexerqueue.azure.util.SbMessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/***
 * A class to create subscription clients and register them with service bus message handling options.
 */
@Component
public class SubscriptionManager {

    @Autowired
    private SubscriptionClientFactory clientFactory;
    @Autowired
    private SbMessageBuilder sbMessageBuilder;
    @Autowired
    private RecordChangedMessageHandler recordChangedMessageHandler;
    @Autowired
    private AzureBootstrapConfig azureBootstrapConfig;
    @Autowired
    private ITenantFactory tenantFactory;
    private ICoreLogger Logger = CoreLoggerFactory.getInstance().getLogger(SubscriptionManager.class.getName());

    /***
     * Create subscription clients for service buses of different partitions to register them with message handling options.
     */
    public void subscribeRecordsTopic() {
        List<String> tenantList = tenantFactory.listTenantInfo().stream().map(TenantInfo::getDataPartitionId)
                .collect(Collectors.toList());

        ExecutorService executorService = Executors
                .newFixedThreadPool(Integer.parseUnsignedInt(azureBootstrapConfig.getNThreads()));

        for (String partition : tenantList) {
            try {
                SubscriptionClient subscriptionClient = this.clientFactory.getSubscriptionClient(partition);
                registerMessageHandler(subscriptionClient, executorService);
                Logger.info(String.format("Successfully registered subscription client for service bus of partition %s", partition));
            }
            catch (Exception e) {
                Logger.warn("Error while creating or registering service bus subscription client for partition {}\n{}", partition, e);
            }

        }
    }
    /*
     * For the given subscriptionClient, register message handler with message handling options as mentioned below.
     * maxAutoRenewDuration --> Maximum duration within which the client keeps renewing the message lock if the processing of the message is not completed by the handler.
     * autoComplete         --> true if the pump should automatically complete message after onMessageHandler action is completed. false otherwise.
     * messageWaitDuration  --> duration to wait for receiving the message.
     * maxConcurrentCalls   --> maximum number of concurrent calls to the onMessage handler. (maximum messages that can be handled at any given point.)
     */
    private void registerMessageHandler(SubscriptionClient subscriptionClient, ExecutorService executorService) {
        try {
            MessageHandler messageHandler = new MessageHandler(subscriptionClient, recordChangedMessageHandler, sbMessageBuilder);
            subscriptionClient.registerMessageHandler(
                    messageHandler,
                    new MessageHandlerOptions(Integer.parseUnsignedInt(azureBootstrapConfig.getMaxConcurrentCalls()),
                            false,
                            Duration.ofSeconds(Integer.parseUnsignedInt(azureBootstrapConfig.getMaxLockRenewDurationInSeconds())),
                            Duration.ofSeconds(1)
                    ),
                    executorService);

        } catch (InterruptedException | ServiceBusException e) {
            Logger.debug("Error registering message handler for subscription named - {}\n error - {}", subscriptionClient.getSubscriptionName() ,e);
        }
    }
}