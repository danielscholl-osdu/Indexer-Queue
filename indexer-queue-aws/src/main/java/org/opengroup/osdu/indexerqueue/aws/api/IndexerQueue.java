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

import javax.jdo.annotations.Index;
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
                    /*
                    Messages without authorization attributes will be removed them from queue and send directly to DLQ
                     */
                    ArrayList<IndexProcessor> deleteMessageList=  new ArrayList<IndexProcessor>();
                    ArrayList<IndexProcessor> deadletters = new ArrayList<>();
                    List<IndexProcessor> nonAuthorizedProcessors = messages
                            .stream()
                            .filter(msg -> !msg.getMessageAttributes().containsKey("authorization"))
                            .map(msg -> new IndexProcessor(msg, indexerUrl, "", CallableResult.Fail))
                            .collect(Collectors.toList());


                    // batching messages for deletion

                    deleteMessageList.addAll(nonAuthorizedProcessors);
                    deadletters.addAll(nonAuthorizedProcessors);


                    List<Message> authorizedMessages = messages
                            .stream()
                            .filter(msg -> {
                                System.out.println(msg.getMessageAttributes());
                                return msg.getMessageAttributes().containsKey("authorization");

                            })
                            .collect(Collectors.toList());

                    /*
                        Max retry handles by AWS SQS. If max number of reads from queue failed to process a given message,
                        the message is automatically sent to DLQ by SQS policy.
                        As long as the IndexerService doesn't add more stuff back to the queue with retry counter and new message ID
                     */
                    // process only authorized messages
                    List<IndexProcessor> indexProcessors = IndexerQueueService.processQueue(authorizedMessages,  indexerUrl, executorPool);
                    System.out.println(String.format("%s Authorized Messages Processed", indexProcessors.size()));

                    List<IndexProcessor> failedProcessors =  indexProcessors.stream().filter(indexProcessor -> indexProcessor.result == CallableResult.Fail || indexProcessor.exception != null).collect(Collectors.toList());
                    System.out.println(String.format("Total %s Messages Failed", failedProcessors.size()));

                    // check if the message exponential visibility timeout
                    IndexerQueueService.ChangeMessageVisibilityTimeout(sqsClient, environmentVariables.queueUrl, failedProcessors);
                    List<SendMessageResult> deadLetterResults = IndexerQueueService.sendMsgsToDeadLetterQueue(environmentVariables.deadLetterQueueUrl, deadletters, sqsClient);
                    System.out.println(String.format("%s Messages Dead Lettered", deadLetterResults.size()));
                    deleteMessageList.addAll(indexProcessors.stream().filter(indexProcessor -> indexProcessor.result==CallableResult.Pass).collect(Collectors.toList()));
                    List<DeleteMessageBatchRequestEntry> deleteEntries = deleteMessageList.stream().map(indexProcessor -> new DeleteMessageBatchRequestEntry(indexProcessor.messageId, indexProcessor.receiptHandle)).collect(Collectors.toList());
                    System.out.println(String.format("%s Messages Deleting (Succeeded and Non-Authorized)", deleteEntries.size()));


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


