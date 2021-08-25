package org.opengroup.osdu.indexerqueue.azure.queue;

import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.SubscriptionClient;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.ICoreLogger;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.azure.scope.thread.ThreadScopeContextHolder;
import org.opengroup.osdu.indexerqueue.azure.util.SbMessageBuilder;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;

/***
 * Defines callback for receiving messages from Azure Service Bus.
 */
public class MessageHandler implements IMessageHandler {

    private SubscriptionClient receiveClient;
    private RecordChangedMessageHandler recordChangedMessageHandler;
    private SbMessageBuilder sbMessageBuilder;
    private RecordChangedMessages recordChangedMessage;
    private ICoreLogger Logger = CoreLoggerFactory.getInstance().getLogger(MessageHandler.class.getName());

    MessageHandler(SubscriptionClient client, RecordChangedMessageHandler recordChangedMessageHandler, SbMessageBuilder sbMessageBuilder) {
        this.receiveClient = client;
        this.recordChangedMessageHandler = recordChangedMessageHandler;
        this.sbMessageBuilder = sbMessageBuilder;
    }

    /*
     * Receives a single batch of messages from service bus and sends to builds it as `RecordChangedMessages` for indexer service.
     * One batch of messages from storage service to service bus has upto 50 `PubSubInfo` messages.
     */
    @Override
    public CompletableFuture<Void> onMessageAsync(IMessage message) {

        String messageBody = new String(message.getMessageBody().getBinaryData().get(0), UTF_8);
        long startTime = System.currentTimeMillis();
        long enqueueTime = message.getEnqueuedTimeUtc().toEpochMilli();
        try {
            recordChangedMessage = sbMessageBuilder.getServiceBusMessage(messageBody);
            recordChangedMessage.setPublishTime(message.getEnqueuedTimeUtc().toString());
            recordChangedMessage.setMessageId(message.getMessageId());
            recordChangedMessageHandler.sendMessagesToIndexer(recordChangedMessage, message.getDeliveryCount());
            Logger.info("Successfully sent recordChangedMessages {} to indexer service.",recordChangedMessage.getData());
            long stopTime = System.currentTimeMillis();
            Logger.info("Execution time: {}", stopTime - startTime);
            Logger.info("End-to-End execution time: {}", stopTime - enqueueTime);
            ThreadScopeContextHolder.getContext().clear();
            MDC.clear();
            return receiveClient.completeAsync(message.getLockToken());

        } catch (Exception e) {
            ThreadScopeContextHolder.getContext().clear();
            MDC.clear();
            if(Instant.now().compareTo(message.getExpiresAtUtc()) < 0) {
                // Current instant is less than message expiry time => message lock has not expired yet.
                // We need to explicitly abandon the message.
                return receiveClient.abandonAsync(message.getLockToken());
            } else {
                // Message lock already expired.
                return null;
            }
        }
    }

    /***
     * Receiving the exceptions that passed by pump during message processing.
     * @param throwable
     * @param exceptionPhase Enumeration to represent the phase in a message pump or session pump at which an exception occurred.
     */
    @Override
    public void notifyException(Throwable throwable, ExceptionPhase exceptionPhase) {
        Logger.error("Exception {} occurred in service bus message in exception phase {}.", exceptionPhase ,throwable.getMessage());
    }
}
