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
import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.aws.sqs.AmazonSQSConfig;

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
     * @param args
     * @throws ExecutionException
     * @throws InterruptedException
     */

    private static final int MAX_MESSAGE_ALLOWED = 100000;
    private static final int MAX_INDEX_THREADS = 50;
    private static final int MAX_BATCH_REQUEST_COUNT=10;
    public static void main(String[] args) {
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        List<Message> messages;
        String indexerUrl = String.format("%s/%s",environmentVariables.targetURL, "api/indexer/v2/_dps/task-handlers/index-worker" );

        // System print lines go to cloudwatch automatically within ECS
        System.out.println("Running Queue processor with the following environment variables:");
        System.out.println(String.format("Region: %s", environmentVariables.region));
        System.out.println(String.format("Queue Name: %s", environmentVariables.queueUrl));
        System.out.println(String.format("Dead letter queue name: %s", environmentVariables.deadLetterQueueUrl));
        System.out.println(String.format("Target url: %s", environmentVariables.targetURL));
        while (true) {

            try {
                System.out.println("Starting Indexer Queue and obtaining Arguments");

                System.out.println(String.format("Connecting to the SQS Queue: %s", environmentVariables.queueUrl));
                AmazonSQSConfig sqsConfig = new AmazonSQSConfig(environmentVariables.region);
                AmazonSQS sqsClient = sqsConfig.AmazonSQS();
                System.out.println(String.format("Creating a thread pool with %s threads", MAX_INDEX_THREADS));
                ThreadPoolExecutor executorPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_INDEX_THREADS);
                System.out.println(String.format("Dead letter queue url: %s", environmentVariables.deadLetterQueueUrl));
                messages = IndexerQueueService.getMessages(sqsClient, environmentVariables.queueUrl, MAX_BATCH_REQUEST_COUNT, MAX_MESSAGE_ALLOWED);
                System.out.println(String.format("Processing %s messages from storage queue", messages.size()));


                if (!messages.isEmpty()) {

                    List<IndexProcessor> nonAuthorizedProcessors = messages
                            .stream()
                            .filter(msg -> !msg.getMessageAttributes().containsKey("authorization"))
                            .map(msg -> new IndexProcessor(msg, indexerUrl, "", CallableResult.Fail))
                            .collect(Collectors.toList());

                    List<Message> authorizedMessages = messages
                            .stream()
                            .filter(msg -> msg.getMessageAttributes().containsKey("authorization"))
                            .collect(Collectors.toList());

                    List<Message> freshMessages = authorizedMessages
                            .stream()
                            .filter(msg -> {
                                if(msg.getMessageAttributes().containsKey("retry")) {
                                    int retryCount = Integer.parseInt(msg.getMessageAttributes().get("retry").getStringValue());
                                    if (retryCount < 10)
                                    {
                                        return true;
                                    }
                                } else {
                                    return true;
                                }
                                return false;
                            }).collect(Collectors.toList());

                    List<IndexProcessor> maxedRetryFailedProcessors= authorizedMessages
                            .stream()
                            .filter(msg -> {
                                if(msg.getMessageAttributes().containsKey("retry")) {
                                    int retryCount = Integer.parseInt(msg.getMessageAttributes().get("retry").getStringValue());
                                    if (retryCount > 9)
                                    {
                                        return true;
                                    }
                                }
                                return false;
                            })
                            .map(msg -> new IndexProcessor(msg, indexerUrl, "", CallableResult.Fail))
                            .collect(Collectors.toList());

                    List<IndexProcessor> indexProcessors = IndexerQueueService.processQueue(freshMessages,  indexerUrl, executorPool);
                    System.out.println(String.format("%s Messages Processed", indexProcessors.size()));

                    List<IndexProcessor> failedProcessors =  indexProcessors.stream().filter(indexProcessor -> indexProcessor.result == CallableResult.Fail || indexProcessor.exception != null).collect(Collectors.toList());
                    failedProcessors.addAll(nonAuthorizedProcessors);
                    failedProcessors.addAll(maxedRetryFailedProcessors);
                    System.out.println(String.format("%s Messages Failed", failedProcessors.size()));

                    List<SendMessageResult> deadLetterResults = IndexerQueueService.sendMsgsToDeadLetterQueue(environmentVariables.deadLetterQueueUrl, failedProcessors, sqsClient);
                    System.out.println(String.format("%s Messages Dead Lettered", deadLetterResults.size()));

                    List<DeleteMessageBatchRequestEntry> deleteEntries = indexProcessors.stream().map(indexProcessor -> new DeleteMessageBatchRequestEntry(indexProcessor.messageId, indexProcessor.receiptHandle)).collect(Collectors.toList());
                    System.out.println(String.format("%s Messages Deleting", deleteEntries.size()));

                    List<DeleteMessageBatchRequest> deleteBatchRequests = IndexerQueueService.createMultipleBatchDeleteRequest(environmentVariables.queueUrl, deleteEntries, MAX_BATCH_REQUEST_COUNT);
                    System.out.println(String.format("%s Delete Batch Request Created", deleteBatchRequests.size()));

                    List<DeleteMessageBatchResult> deleteMessageBatchResults = IndexerQueueService.deleteMessages(deleteBatchRequests, sqsClient);
                    System.out.println(String.format("%s Requests Deleted", deleteMessageBatchResults.size()));
                }

                Thread.sleep(5000);

            } catch (ExecutionException e) {
                System.out.println(e.getMessage());
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            } catch (NullPointerException e) {
                System.out.println(e.getMessage());
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
