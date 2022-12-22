package org.opengroup.osdu.indexerqueue.azure.queue;

import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.opengroup.osdu.azure.servicebus.ITopicClientFactory;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.indexerqueue.azure.config.ThreadDpsHeaders;
import org.opengroup.osdu.indexerqueue.azure.di.AzureBootstrapConfig;
import org.opengroup.osdu.indexerqueue.azure.metrics.IMetricService;
import org.opengroup.osdu.indexerqueue.azure.util.MdcContextMap;
import org.opengroup.osdu.indexerqueue.azure.util.MessageAttributesExtractor;
import org.opengroup.osdu.indexerqueue.azure.util.RetryUtil;
import org.opengroup.osdu.indexerqueue.azure.util.SbMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private ITopicClientFactory topicClientFactory;
    @Autowired
    private RetryUtil retryUtil;
    @Autowired
    private SbMessageBuilder sbMessageBuilder;
    @Autowired
    private RecordChangedMessageHandler recordChangedMessageHandler;
    @Autowired
    private AzureBootstrapConfig azureBootstrapConfig;
    @Autowired
    private ITenantFactory tenantFactory;
    @Autowired
    private IMetricService metricService;
    @Autowired
    private RetryTemplate retryTemplate;

    @Autowired
    private ThreadDpsHeaders dpsHeaders;
    @Autowired
    private MdcContextMap mdcContextMap;
    @Autowired
    private MessageAttributesExtractor messageAttributesExtractor;
    private final Logger logger = LoggerFactory.getLogger(SubscriptionManager.class.getName());

    /***
     * Create subscription clients for service buses of different partitions to register them with message handling options.
     */
    public void subscribeRecordsTopic() {

        Set<String> partitions = new HashSet<>();
        ExecutorService executorService = Executors
                .newFixedThreadPool(Integer.parseUnsignedInt(azureBootstrapConfig.getNThreads()));
        Integer sleepMainThreadDuration = azureBootstrapConfig.getSleepDurationForMainThreadInSeconds();

        while(true) {

            try {
                fetchPartitionsAndSubscribe(executorService, partitions);
            }
            catch (Exception e) {
                logger.error("Exception encountered while fetching partition information", e);
            }

            try {
                logger.info("Sleeping the main thread...");
                Thread.sleep(sleepMainThreadDuration * 1000);
            }
            catch (Exception e) {
                logger.error("Exception encountered while sleeping the main thread", e);
                break;
            }
        }

    }

    public void fetchPartitionsAndSubscribe(ExecutorService executorService, Set<String> partitions) {
        List<String> tenantList = tenantFactory.listTenantInfo().stream().map(TenantInfo::getDataPartitionId)
                .collect(Collectors.toList());

        logger.info("Total number of partitions " + tenantList.size());

        for (String partition : tenantList) {

            if(partitions.contains(partition)) {
                continue;
            }
            try {
                SubscriptionClient subscriptionClient = this.clientFactory.getSubscriptionClient(partition);
                MessagePublisher messagePublisher = new TopicMessagePublisher(this.topicClientFactory, retryTemplate,
                        partition, azureBootstrapConfig.getServiceBusTopic());
                registerMessageHandler(subscriptionClient, messagePublisher, executorService);
                partitions.add(partition);
            }
            catch (Exception e) {
                logger.error("Error while creating or registering subscription client", e);
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
    private void registerMessageHandler(SubscriptionClient subscriptionClient, MessagePublisher messageSender, ExecutorService executorService) {
        try {
            Integer maxDeliveryCount = Integer.valueOf(azureBootstrapConfig.getMaxDeliveryCount());
            String appName = azureBootstrapConfig.getAppName();
            MessageHandler messageHandler = new MessageHandler(subscriptionClient,
                    messageSender, recordChangedMessageHandler, sbMessageBuilder,
                    metricService, retryUtil, dpsHeaders, mdcContextMap, messageAttributesExtractor, maxDeliveryCount, appName);
            subscriptionClient.registerMessageHandler(
                    messageHandler,
                    new MessageHandlerOptions(Integer.parseUnsignedInt(azureBootstrapConfig.getMaxConcurrentCalls()),
                            false,
                            Duration.ofSeconds(Integer.parseUnsignedInt(azureBootstrapConfig.getMaxLockRenewDurationInSeconds())),
                            Duration.ofSeconds(1)
                    ),
                    executorService);

        } catch (InterruptedException | ServiceBusException e) {
            logger.debug("Error registering message handler for subscription named - {}\n error - {}", subscriptionClient.getSubscriptionName() ,e);
        }
    }
}
