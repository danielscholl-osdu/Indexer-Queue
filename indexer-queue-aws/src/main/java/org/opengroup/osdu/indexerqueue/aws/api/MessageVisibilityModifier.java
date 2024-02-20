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
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MessageVisibilityModifier extends MessageHandler<ChangeMessageVisibilityBatchRequestEntry> {

    private final String sqsQueueURL;

    public MessageVisibilityModifier(BlockingQueue<Message> messagesToHandle, int maxBatchRequests, AmazonSQS sqsClient, String sqsQueueURL) {
        super(messagesToHandle, maxBatchRequests, sqsClient);
        this.sqsQueueURL = sqsQueueURL;
    }

    @Override
    protected ChangeMessageVisibilityBatchRequestEntry generateHandleRequest(Message message) {
        ChangeMessageVisibilityBatchRequestEntry entry = new ChangeMessageVisibilityBatchRequestEntry(message.getMessageId(), message.getReceiptHandle());
        entry.setVisibilityTimeout(getExponentialTimeOutWindow(message));
        return entry;
    }

    private int getExponentialTimeOutWindow(Message message){
        int receiveCount;
        try {
           receiveCount = Integer.parseInt(message.getAttributes().get("ApproximateReceiveCount"));
        } catch (NumberFormatException e) {
            receiveCount = 0;
        }
        // max receive count to 10 in SQS setting
        switch (receiveCount) {
            case 0: case 1: case 2: return 5;
            case 3: case 4: return 10;
            case 5: case 6: return 30;
            case 7: case 8: return 60;
            case 9: case 10: return 90;
            default: return 120;
        }
    }
    @Override
    protected void handleRequestBatch(List<ChangeMessageVisibilityBatchRequestEntry> batch, AmazonSQS sqsClient) {
        sqsClient.changeMessageVisibilityBatch(sqsQueueURL, batch);
    }
}
