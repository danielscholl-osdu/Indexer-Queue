// Copyright © Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexerqueue.azure.queue;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.SubscriptionClient;
import org.opengroup.osdu.azure.servicebus.AbstractMessageHandler;
import org.opengroup.osdu.indexerqueue.azure.config.ThreadDpsHeaders;
import org.opengroup.osdu.indexerqueue.azure.scope.thread.ThreadScopeContextHolder;
import org.opengroup.osdu.indexerqueue.azure.util.MdcContextMap;
import org.opengroup.osdu.indexerqueue.azure.util.MessageAttributesExtractor;
import org.opengroup.osdu.indexerqueue.azure.util.RecordChangedAttributes;
import org.opengroup.osdu.indexerqueue.azure.util.RetryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;

/***
 * Abstract class that implements IMessageHandler. This class cannot be instantiated and is created to enforce standard logging practises for workers.
 */
public abstract class AbstractMessageHandlerWithActiveRetry extends AbstractMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessageHandlerWithActiveRetry.class.getName());
    private static final String PROPERTY_RETRY = "RETRY";
    private static final String RETRY_LOG_MESSAGE_TEMPLATE = "Exception occurred while sending message %s to indexer service: %s - %s.";
    private final String workerName;
    private final SubscriptionClient receiveClient;
    private final MessagePublisher messagePublisher;
    private final Integer maxDeliveryCount;
    private final RetryUtil retryUtil;
    private final ThreadDpsHeaders dpsHeaders;
    private final MdcContextMap mdcContextMap;
    private final MessageAttributesExtractor messageAttributesExtractor;

    /***
     * Constructor.
     * @param workerServiceName name of the worker service.
     * @param client subscription client.
     * @param publisher publisher for sending messages to topic
     * @param maximumDeliveryCount retries threshold
     */
    public AbstractMessageHandlerWithActiveRetry(final SubscriptionClient client,
                                                 final MessagePublisher publisher,
                                                 final RetryUtil retryUtil,
                                                 final ThreadDpsHeaders dpsHeaders,
                                                 final MdcContextMap mdcContextMap,
                                                 final MessageAttributesExtractor messageAttributesExtractor,
                                                 final String workerServiceName,
                                                 final Integer maximumDeliveryCount) {
        super(workerServiceName, client);
        this.receiveClient = client;
        this.messagePublisher = publisher;
        this.workerName = workerServiceName;
        this.maxDeliveryCount = maximumDeliveryCount;
        this.retryUtil = retryUtil;
        this.dpsHeaders = dpsHeaders;
        this.mdcContextMap = mdcContextMap;
        this.messageAttributesExtractor = messageAttributesExtractor;
    }

    /***
     * Receives a message from service bus and processes it.
     * @param message service bus message.
     * @return a CompletableFuture representing the pending complete.
     */
    @Override
    public CompletableFuture<Void> onMessageAsync(final IMessage message) {
        long startTime = System.currentTimeMillis();
        long enqueueTime = message.getEnqueuedTimeUtc().toEpochMilli();
        String messageId = message.getMessageId();
      try {
            setupLoggerContext(message);
            logWorkerStart(messageId, this.workerName, "Received message from service bus");
            processMessage(message);
            long stopTime = System.currentTimeMillis();
            logWorkerEnd(messageId, this.workerName, String.format("Successfully processed message. End to end time from enqueue : %d", stopTime - enqueueTime), stopTime - startTime, true);
            if (message.getProperties().get(PROPERTY_RETRY) != null) {
                Integer retryValue = (Integer) message.getProperties().get(PROPERTY_RETRY);
                String messageBody = new String(message.getMessageBody().getBinaryData().get(0), UTF_8);
                LOGGER.info("Successfully sent message {} after {} retries", messageBody, retryValue);
            }
            return this.receiveClient.completeAsync(message.getLockToken());

        } catch (Exception e) {
            String messageBody = new String(message.getMessageBody().getBinaryData().get(0), UTF_8);
            if (message.getProperties().get(PROPERTY_RETRY) == null) {
                int retryDuration = retryUtil.generateNextRetryTerm(1);
                message.getProperties().put(PROPERTY_RETRY, 1);
                Instant utcEnqueueTime = Clock.systemUTC().instant().plus(retryDuration, SECONDS);
                logMessageForRetry(messageBody, e, 1, false);
                return ackMessageWithRetry(() -> receiveClient.completeAsync(message.getLockToken()), message, utcEnqueueTime);
            } else {
                Integer retryValue = (Integer) message.getProperties().get(PROPERTY_RETRY);
                if (retryValue > maxDeliveryCount) {
                    logMessageForRetry(messageBody, e, retryValue, true);
                    return receiveClient.deadLetterAsync(message.getLockToken());
                } else {
                    retryValue++;
                    message.getProperties().put(PROPERTY_RETRY, retryValue);
                    int retryDuration = retryUtil.generateNextRetryTerm(retryValue);
                    Instant utcEnqueueTime = Clock.systemUTC().instant().plus(retryDuration, SECONDS);
                    logMessageForRetry(messageBody, e, retryValue, false);
                    return ackMessageWithRetry(() -> receiveClient.completeAsync(message.getLockToken()), message, utcEnqueueTime);
                }
            }
        } finally {
            cleanUpLoggerContext();
        }
    }

    private void logMessageForRetry(String messageBody, Exception e, int retryNumber, boolean isLastRetry) {
        if (isLastRetry) {
            LOGGER.error(String.format(RETRY_LOG_MESSAGE_TEMPLATE + " Retries limit exceed and message will be sent to dead letter queue", e.getClass().getName(), e.getMessage(), e.getMessage()));
        } else {
            LOGGER.warn(String.format(RETRY_LOG_MESSAGE_TEMPLATE + " Retry number %d for the message: ",
                    messageBody, e.getClass().getName(), e.getMessage() , retryNumber));
        }
    }


    /**
     * @param supplier                the supplier which should be invoked
     * @param message                 servicebus topic message
     * @param scheduledEnqueueTimeUTC enqueue time for new message
     * @return CompletableFuture<Void>
     */
    private CompletableFuture<Void> ackMessageWithRetry(final Supplier<CompletableFuture<Void>> supplier,
                                                        final IMessage message,
                                                        final Instant scheduledEnqueueTimeUTC) {
        try {
            return supplier.get();
        } finally {
            try {
                messagePublisher.sendMessageToTopic(message, scheduledEnqueueTimeUTC);
            } catch (Exception e) {
                receiveClient.deadLetterAsync(message.getLockToken());
            }
        }
    }

    private void setupLoggerContext(IMessage message) {
        RecordChangedAttributes recordChangedAttributes = messageAttributesExtractor.extractAttributesFromMessageBody(message);
        String correlationId = recordChangedAttributes.getCorrelationId();
        String dataPartitionId = recordChangedAttributes.getDataPartitionId();
        MDC.setContextMap(mdcContextMap.getContextMap(correlationId, dataPartitionId));
        dpsHeaders.setThreadContext(dataPartitionId, correlationId);
    }

    private void cleanUpLoggerContext() {
        ThreadScopeContextHolder.getContext().clear();
        MDC.clear();
    }
}
