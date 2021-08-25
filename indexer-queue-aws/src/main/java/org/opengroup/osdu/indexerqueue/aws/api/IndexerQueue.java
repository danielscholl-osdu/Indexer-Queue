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
import org.opengroup.osdu.core.aws.sqs.AmazonSQSConfig;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

public class IndexerQueue {
    /**
     * int maxIndexThreads = 10;
     * String region = "us-east-1";
     * String queueName = "dev-osdu-storage-queue";
     * String targetURL = "http://127.0.0.1:8080/api/indexer/v2/_dps/task-handlers/index-worker";
     *
     * @param args
     * @throws ExecutionException
     * @throws InterruptedException
     */

    private static final int MAX_MESSAGE_ALLOWED = 100000;
    private static final int MAX_INDEX_THREADS = 50;
    private static final int MAX_BATCH_REQUEST_COUNT = 10;

    public static void main(String[] args) throws K8sParameterNotFoundException {
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        List<Message> messages;
        String indexerUrl = String.format("%s/%s", environmentVariables.targetURL, "api/indexer/v2/_dps/task-handlers/index-worker");
        String reIndexUrl = String.format("%s/%s", environmentVariables.targetURL, "api/indexer/v2/reindex?force_clean=false");
        // System print lines go to cloudwatch automatically within ECS
        System.out.println("Running Queue processor with the following environment variables:");
        System.out.printf("Region: %s%n", environmentVariables.region);
        System.out.printf("Queue Name: %s%n", environmentVariables.queueUrl);
        System.out.printf("Dead letter queue name: %s%n", environmentVariables.deadLetterQueueUrl);
        System.out.printf("Target url: %s%n", environmentVariables.targetURL);
        while (true) {

            try {
                System.out.println("Starting Indexer Queue and obtaining Arguments");

                System.out.printf("Connecting to the SQS Queue: %s%n", environmentVariables.queueUrl);
                AmazonSQSConfig sqsConfig = new AmazonSQSConfig(environmentVariables.region);
                AmazonSQS sqsClient = sqsConfig.AmazonSQS();
                System.out.printf("Creating a thread pool with %s threads%n", MAX_INDEX_THREADS);
                ThreadPoolExecutor executorPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_INDEX_THREADS);
                System.out.printf("Dead letter queue url: %s%n", environmentVariables.deadLetterQueueUrl);
                messages = IndexerQueueService.getMessages(sqsClient, environmentVariables.queueUrl, MAX_BATCH_REQUEST_COUNT, MAX_MESSAGE_ALLOWED);
                System.out.printf("Processing %s messages from storage queue%n", messages.size());


                if (!messages.isEmpty()) {
                    List<Message> reIndexMessages = messages.stream().filter(
                            msg -> msg.getMessageAttributes().containsKey("ReIndexCursor")
                    ).collect(Collectors.toList());
                    System.out.printf("Size of Reindex message list is %s %n", reIndexMessages.size());
                    processReIndexMessages(reIndexMessages, reIndexUrl, environmentVariables.queueUrl, executorPool, sqsClient);
                    messages.removeAll(reIndexMessages);
                    processIndexMessages(messages, indexerUrl, environmentVariables.queueUrl, environmentVariables.deadLetterQueueUrl, executorPool, sqsClient);
                    executorPool.shutdown();
                }

                Thread.sleep(5000);

            } catch (ExecutionException e) {
                System.out.println(e.getMessage());
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            } catch (NullPointerException e) {
                System.out.println(e.getMessage());
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

        }
    }

    public static void processIndexMessages(List<Message> messages, String indexerUrl, String queueUrl, String deadLetterQueueUrl, ThreadPoolExecutor executorPool, AmazonSQS sqsClient) throws ExecutionException, InterruptedException {
        System.out.printf("Processing %s IndexMessage%n", messages.size());
         /*
            Messages without authorization attributes will be removed them from queue and send directly to DLQ
         */
        List<IndexProcessor> nonAuthorizedProcessors = messages.stream()
                .filter(msg -> !msg.getMessageAttributes().containsKey("authorization"))
                .map(msg -> new IndexProcessor(msg, indexerUrl, "", CallableResult.Fail))
                .collect(Collectors.toList());
        // batching messages for deletion
        ArrayList<IndexProcessor> deleteMessageList = new ArrayList<>(nonAuthorizedProcessors);
        ArrayList<IndexProcessor> deadletters = new ArrayList<>(nonAuthorizedProcessors);

        List<Message> authorizedMessages = messages.stream()
                .filter(msg -> {
                    return msg.getMessageAttributes().containsKey("authorization");
                })
                .collect(Collectors.toList());
        /*
            Max retry handles by AWS SQS. If max number of reads from queue failed to process a given message,
            the message is automatically sent to DLQ by SQS policy.
            As long as the IndexerService doesn't add more stuff back to the queue with retry counter and new message ID
         */
        // process only authorized messages
        List<IndexProcessor> indexProcessors = IndexerQueueService.processIndexQueue(authorizedMessages, indexerUrl, executorPool);
        System.out.printf("%s Authorized Index Messages Processed%n", indexProcessors.size());

        List<IndexProcessor> failedProcessors = indexProcessors.stream().filter(indexProcessor -> indexProcessor.result == CallableResult.Fail || indexProcessor.exception != null).collect(Collectors.toList());
        System.out.printf("Total %s Index Messages Failed%n", failedProcessors.size());

        // check if the message exponential visibility timeout
        IndexerQueueService.ChangeMessageVisibilityTimeout(sqsClient, queueUrl, failedProcessors);
        List<SendMessageResult> deadLetterResults = IndexerQueueService.sendMsgsToDeadLetterQueue(deadLetterQueueUrl, deadletters, sqsClient);
        System.out.printf("%s Index Messages Dead Lettered%n", deadLetterResults.size());
        deleteMessageList.addAll(indexProcessors.stream().filter(indexProcessor -> indexProcessor.result == CallableResult.Pass).collect(Collectors.toList()));
        List<DeleteMessageBatchRequestEntry> deleteEntries = deleteMessageList.stream().map(indexProcessor -> new DeleteMessageBatchRequestEntry(indexProcessor.messageId, indexProcessor.receiptHandle)).collect(Collectors.toList());
        System.out.printf("%s Index Messages Deleting (Succeeded and Non-Authorized)%n", deleteEntries.size());
        List<DeleteMessageBatchRequest> deleteBatchRequests = IndexerQueueService.createMultipleBatchDeleteRequest(queueUrl, deleteEntries, MAX_BATCH_REQUEST_COUNT);
        System.out.printf("%s Delete Batch Request Created%n", deleteBatchRequests.size());

        List<DeleteMessageBatchResult> deleteMessageBatchResults = IndexerQueueService.deleteMessages(deleteBatchRequests, sqsClient);
        System.out.printf("%s Requests Deleted%n", deleteMessageBatchResults.size());
    }

    public static void processReIndexMessages(List<Message> reIndexMessages, String reIndexUrl, String queueUrl, ThreadPoolExecutor executorPool, AmazonSQS sqsClient) throws ExecutionException, InterruptedException {
        List<ReIndexProcessor> reIndexProcessors = IndexerQueueService.processReIndexQueue(reIndexMessages, reIndexUrl, executorPool);
        List<ReIndexProcessor> failedProcessors = reIndexProcessors.stream().filter(indexProcessor -> indexProcessor.result == CallableResult.Fail || indexProcessor.exception != null).collect(Collectors.toList());
        System.out.printf("Total %s ReIndex Messages Failed%n", failedProcessors.size());
        reIndexProcessors.removeAll(failedProcessors);
        ArrayList<ReIndexProcessor> deleteMessageList = new ArrayList<>();
        deleteMessageList.addAll(reIndexProcessors.stream().filter(indexProcessor -> indexProcessor.result == CallableResult.Pass).collect(Collectors.toList()));
        List<DeleteMessageBatchRequestEntry> deleteEntries = deleteMessageList.stream().map(indexProcessor -> new DeleteMessageBatchRequestEntry(indexProcessor.messageId, indexProcessor.receiptHandle)).collect(Collectors.toList());
        System.out.printf("%s ReIndex Messages Deleting (Succeeded and Non-Authorized)%n", deleteEntries.size());
        List<DeleteMessageBatchRequest> deleteBatchRequests = IndexerQueueService.createMultipleBatchDeleteRequest(queueUrl, deleteEntries, MAX_BATCH_REQUEST_COUNT);
        System.out.printf("%s Delete Batch Request Created%n", deleteBatchRequests.size());
        List<DeleteMessageBatchResult> deleteMessageBatchResults = IndexerQueueService.deleteMessages(deleteBatchRequests, sqsClient);
        System.out.printf("%s Requests Deleted%n", deleteMessageBatchResults.size());

    }
}


