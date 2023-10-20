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

    private IndexerQueueService() {
        // Private constructor
    }

    public static List<IndexProcessor> processIndexQueue(List<Message> messages, String url, ThreadPoolExecutor executorPool)
            throws ExecutionException, InterruptedException {

        List<CompletableFuture<IndexProcessor>> futures = createIndexCompletableFutures(messages, executorPool, url);

        CompletableFuture[] cfs = futures.toArray(new CompletableFuture[0]);
        CompletableFuture<List<IndexProcessor>> results = CompletableFuture
                .allOf(cfs)
                .thenApply(ignored -> futures
                        .stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));

//        List<IndexProcessor> processed = results.get();
//        executorPool.shutdown();

        return results.get();
    }
    public static List<ReIndexProcessor> processReIndexQueue(List<Message> messages, String url, ThreadPoolExecutor executorPool)
            throws ExecutionException, InterruptedException {

        List<CompletableFuture<ReIndexProcessor>> futures = createReIndexCompletableFutures(messages, executorPool, url);

        CompletableFuture[] cfs = futures.toArray(new CompletableFuture[0]);
        CompletableFuture<List<ReIndexProcessor>> results = CompletableFuture
                .allOf(cfs)
                .thenApply(ignored -> futures
                        .stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));

//        List<ReIndexProcessor> processed = results.get();
//        executorPool.shutdown();

        return results.get();
    }
    public static List<DeleteMessageBatchResult> deleteMessages(List<DeleteMessageBatchRequest> deleteBatchRequests, AmazonSQS sqsClient) {
        return deleteBatchRequests.stream().map(deleteRequest -> sqsClient.deleteMessageBatch(deleteRequest)).collect(Collectors.toList());
    }


    public static List<DeleteMessageBatchRequest> createMultipleBatchDeleteRequest(String queueUrl, List<DeleteMessageBatchRequestEntry> deleteEntries, int maxBatchRequest) {
        List<List<DeleteMessageBatchRequestEntry>> batchedEntries = ListUtils.partition(deleteEntries, maxBatchRequest);
        return batchedEntries.stream().map(entries -> new DeleteMessageBatchRequest(queueUrl, entries)).collect(Collectors.toList());
    }

    public static List<CompletableFuture<IndexProcessor>> createIndexCompletableFutures(List<Message> messages, ThreadPoolExecutor executorPool, String url){
        List<CompletableFuture<IndexProcessor>> futures = new ArrayList<>();

        for (final Message message : messages) {
            String indexerServiceAccountJWT = message.getMessageAttributes().get("authorization").getStringValue();
            IndexProcessor processor = new IndexProcessor(message, url, indexerServiceAccountJWT);
            CompletableFuture<IndexProcessor> future = CompletableFuture.supplyAsync(processor::call, executorPool);
            futures.add(future);
        }
        return futures;
    }
    public static List<CompletableFuture<ReIndexProcessor>> createReIndexCompletableFutures(List<Message> messages, ThreadPoolExecutor executorPool, String url){
        List<CompletableFuture<ReIndexProcessor>> futures = new ArrayList<>();

        for (final Message message : messages) {
            String indexerServiceAccountJWT = message.getMessageAttributes().get("authorization").getStringValue();
            ReIndexProcessor processor = new ReIndexProcessor(message, url, indexerServiceAccountJWT);
            CompletableFuture<ReIndexProcessor> future = CompletableFuture.supplyAsync(processor::call, executorPool);
            futures.add(future);
        }
        return futures;
    }

    public static List<Message> getMessages(AmazonSQS sqsClient, String sqsQueueUrl, int numOfmessages, int maxMessageCount){
        int numOfMessages = numOfmessages;
        List<Message> messages = new ArrayList<>();
        do {
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(sqsQueueUrl);
            receiveMessageRequest.setMaxNumberOfMessages(numOfMessages);
            receiveMessageRequest.withMessageAttributeNames("All");
            receiveMessageRequest.withAttributeNames("All");
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
        Map<String, MessageAttributeValue> messageAttributes = indexProcessor.getMessage().getMessageAttributes();
        MessageAttributeValue exceptionAttribute = new MessageAttributeValue()
                .withDataType("String");

        if(indexProcessor.expectionExists()){
            exceptionMessage = indexProcessor.getException().getMessage();
        } else {
            exceptionMessage = "Empty";
        }

        String exceptionAsString = String.format("Exception message: %s", exceptionMessage);
        exceptionAttribute.setStringValue(exceptionAsString);
        messageAttributes.put("Exception", exceptionAttribute);
        SendMessageRequest sendMsgRequest = new SendMessageRequest()
                .withQueueUrl(deadLetterQueueUrl)
                .withMessageBody(indexProcessor.getMessage().getBody())
                .withMessageAttributes(messageAttributes);
        return sqsClient.sendMessage(sendMsgRequest);
    }
    public static void changeMessageVisibilityTimeout(AmazonSQS sqsClient, String sqsQueueUrl,List<IndexProcessor> indexProcessors){

        List<ChangeMessageVisibilityBatchRequestEntry> entries =
                new ArrayList<>();
        for (IndexProcessor indexProcessor : indexProcessors){
            entries.add(
                    new ChangeMessageVisibilityBatchRequestEntry(indexProcessor.getMessageId(), indexProcessor.getMessage().getReceiptHandle())
                            .withVisibilityTimeout(
                                    exponentialTimeOutWindow(parseIntOrDefault(indexProcessor.getMessage().getAttributes().get("ApproximateReceiveCount"), 0))
                            )
            );

            if (entries.size() == 10) { //max batch size for message visibility is 10, split entries by that amount
                sqsClient.changeMessageVisibilityBatch(sqsQueueUrl, entries);
                entries.clear();
            }
        }

        if (!entries.isEmpty())
            sqsClient.changeMessageVisibilityBatch(sqsQueueUrl, entries);

    }
    private static Integer exponentialTimeOutWindow(int receiveCount){
        // max receive count to 10 in SQS setting
        switch (receiveCount){
            case 0: case 1: case 2: return 5;
            case 3: case 4: return 10;
            case 5: case 6: return 30;
            case 7: case 8: return 60;
            case 9: case 10: return 90;
            default: return 120;
        }
    }

    private static Integer parseIntOrDefault(String toParse, int defaultValue) {
        try {
            return Integer.parseInt(toParse);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
