/* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexerqueue.aws.api;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class MessageHandler<T> implements Runnable {
    private final BlockingQueue<Message> messagesToHandle;
    private final int maxBatchRequests;
    private final AmazonSQS sqsClient;
    private final List<T> toHandle;
    private final Set<String> currentMessageIds;
    private final int maxWaitForMessage;
    private final int maxWaitForMessageBatch;
    private Instant oldestCurrentMessage = null;
    protected static final JaxRsDpsLog logger = LogProvider.getLogger();
    static final int MAX_WAIT_FOR_MESSAGE_MILLIS = 10000;
    static final int MAX_WAIT_FOR_MESSAGE_BATCH = 60000;

    protected MessageHandler(BlockingQueue<Message> messagesToHandle, int maxBatchRequests, AmazonSQS sqsClient) {
        this(messagesToHandle, maxBatchRequests, sqsClient, MAX_WAIT_FOR_MESSAGE_MILLIS, MAX_WAIT_FOR_MESSAGE_BATCH);
    }

    protected MessageHandler(BlockingQueue<Message> messagesToHandle, int maxBatchRequests, AmazonSQS sqsClient, int maxWaitForMessage, int maxWaitForMessageBatch) {
        this.messagesToHandle = messagesToHandle;
        this.maxBatchRequests = maxBatchRequests;
        this.sqsClient = sqsClient;
        this.maxWaitForMessage = maxWaitForMessage;
        this.maxWaitForMessageBatch = maxWaitForMessageBatch;
        toHandle = new ArrayList<>(maxBatchRequests);
        currentMessageIds = new HashSet<>(maxBatchRequests);
    }

    protected abstract T generateHandleRequest(Message message);
    protected abstract void handleRequestBatch(List<T> batch, AmazonSQS sqsClient);

    private boolean processMessageAdd(Message newMessage, Instant current) {
        boolean shouldAdd = currentMessageIds.add(newMessage.getMessageId());
        if (shouldAdd) {
            toHandle.add(generateHandleRequest(newMessage));
            if (oldestCurrentMessage == null)
                oldestCurrentMessage = current;
        } else {
            logger.error(String.format("Message with Id %s already exists in batch! Skipping processing!", newMessage.getMessageId()));
        }
        return (toHandle.size() >= maxBatchRequests);
    }

    @Override
    public void run() {
        boolean shouldLoop = true;
        while (shouldLoop) {
            try {
                Message message = messagesToHandle.poll(this.maxWaitForMessage, TimeUnit.MILLISECONDS);
                boolean shouldProcessBatch = false;
                Instant current = Instant.now();
                if (message != null) {
                    shouldProcessBatch = processMessageAdd(message, current);
                }

                if (oldestCurrentMessage != null && current.minusMillis(this.maxWaitForMessageBatch).isAfter(oldestCurrentMessage)) {
                    shouldProcessBatch = true;
                }

                if (shouldProcessBatch) {
                    handleMessageBatch();
                }
            } catch (InterruptedException e) {
                logger.error("Unknown error occured when trying to delete messages.", e);
                shouldLoop = false;
            }
        }
    }

    private void handleMessageBatch() {
        handleRequestBatch(toHandle, sqsClient);
        toHandle.clear();
        currentMessageIds.clear();
        oldestCurrentMessage = null;
    }
}
