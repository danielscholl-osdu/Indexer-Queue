// Copyright © Amazon Web Services
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

package org.opengroup.osdu.indexerqueue.aws.api;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import org.apache.commons.collections4.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

public class IndexerQueueService {

    public static List<IndexProcessor> processQueue(List<Message> messages, String url, ThreadPoolExecutor executorPool)
            throws ExecutionException, InterruptedException {

        List<CompletableFuture<IndexProcessor>> futures = createCompletableFutures(messages, executorPool, url);

        CompletableFuture[] cfs = futures.toArray(new CompletableFuture[0]);
        CompletableFuture<List<IndexProcessor>> results = CompletableFuture
                .allOf(cfs)
                .thenApply(ignored -> futures
                        .stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));

        List<IndexProcessor> processed = results.get();
        executorPool.shutdown();

        return processed;
    }

    public static List<DeleteMessageBatchResult> deleteMessages(List<DeleteMessageBatchRequest> deleteBatchRequests, AmazonSQS sqsClient) {
        return deleteBatchRequests.stream().map(deleteRequest -> sqsClient.deleteMessageBatch(deleteRequest)).collect(Collectors.toList());
    }


    public static List<DeleteMessageBatchRequest> createMultipleBatchDeleteRequest(String queueUrl, List<DeleteMessageBatchRequestEntry> deleteEntries, int maxBatchRequest) {
        List<List<DeleteMessageBatchRequestEntry>> batchedEntries = ListUtils.partition(deleteEntries, maxBatchRequest);
        return batchedEntries.stream().map(entries -> new DeleteMessageBatchRequest(queueUrl, entries)).collect(Collectors.toList());
    }

    public static List<CompletableFuture<IndexProcessor>> createCompletableFutures(List<Message> messages, ThreadPoolExecutor executorPool, String url){
        List<CompletableFuture<IndexProcessor>> futures = new ArrayList<>();

        for (final Message message : messages) {
            String indexerServiceAccountJWT = message.getMessageAttributes().get("authorization").getStringValue();
            System.out.println(message);
            System.out.println(url);
            System.out.println(indexerServiceAccountJWT);
            IndexProcessor processor = new IndexProcessor(message, url, indexerServiceAccountJWT);
            CompletableFuture<IndexProcessor> future = CompletableFuture.supplyAsync(processor::call, executorPool);
            futures.add(future);
        }
        return futures;
    }

    public static List<Message> getMessages(AmazonSQS sqsClient, String queueName, int numOfmessages, int maxMessageCount){
        final String sqsQueueUrl = sqsClient.getQueueUrl(queueName).getQueueUrl();
        System.out.println("inside get messages");
        System.out.println(sqsQueueUrl);
        int numOfMessages = numOfmessages;
        List<Message> messages = new ArrayList<>();
        do {
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(sqsQueueUrl);
            receiveMessageRequest.setMaxNumberOfMessages(numOfMessages);
            receiveMessageRequest.withMessageAttributeNames("All");
            List<Message> retrievedMessages = sqsClient.receiveMessage(receiveMessageRequest).getMessages();
            messages.addAll(retrievedMessages);
            numOfMessages = retrievedMessages.size();

        }while (messages.size() < maxMessageCount && numOfMessages > 0);

        return messages;
    }

    public static List<SendMessageResult> sendMsgsToDeadLetterQueue(String deadLetterQueueUrl, List<IndexProcessor> indexProcessors, AmazonSQS sqsClient) {
        return indexProcessors.stream().map(indexProcessor -> sendMsgToDeadLetterQueue(deadLetterQueueUrl, indexProcessor, sqsClient)).collect(Collectors.toList());
    }

    private static SendMessageResult sendMsgToDeadLetterQueue(String deadLetterQueueUrl, IndexProcessor indexProcessor, AmazonSQS sqsClient){
        String exceptionMessage;
        Map<String, MessageAttributeValue> messageAttributes = indexProcessor.message.getMessageAttributes();
        MessageAttributeValue exceptionAttribute = new MessageAttributeValue()
                .withDataType("String");

        if(indexProcessor.expectionExists()){
            exceptionMessage = indexProcessor.exception.getMessage();
        } else {
            exceptionMessage = "Empty";
        }

        String exceptionAsString = String.format("Exception message: %s", exceptionMessage);
        exceptionAttribute.setStringValue(exceptionAsString);
        messageAttributes.put("Exception", exceptionAttribute);
        SendMessageRequest send_msg_request = new SendMessageRequest()
                .withQueueUrl(deadLetterQueueUrl)
                .withMessageBody(indexProcessor.message.getBody())
                .withMessageAttributes(messageAttributes);
        return sqsClient.sendMessage(send_msg_request);
    }
}
