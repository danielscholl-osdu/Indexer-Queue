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
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MessageRetrier extends MessageHandler<SendMessageBatchRequestEntry> {

    private final String retryQueueURL;

    public MessageRetrier(BlockingQueue<Message> messagesToRetry, int maxBatchRequests, AmazonSQS sqsClient, String retryQueueURL) {
        super(messagesToRetry, maxBatchRequests, sqsClient);
        this.retryQueueURL = retryQueueURL;
    }

    @Override
    protected SendMessageBatchRequestEntry generateHandleRequest(Message message) {
        return new SendMessageBatchRequestEntry().withMessageBody(message.getBody()).withMessageAttributes(message.getMessageAttributes());
    }

    @Override
    protected void handleRequestBatch(List<SendMessageBatchRequestEntry> batch, AmazonSQS sqsClient) {
        SendMessageBatchResult response = sqsClient.sendMessageBatch(retryQueueURL, batch);
        logger.info(String.format("%s Index Messages Dead Lettered", response.getSuccessful().size()));
        int errored = response.getFailed().size();
        if (errored > 0) {
            logger.error(String.format("%d Index Messages could not be Dead Lettered", errored));
        }
    }
}
