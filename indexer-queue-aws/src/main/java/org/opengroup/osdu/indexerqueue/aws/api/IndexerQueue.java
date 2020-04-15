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

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import org.opengroup.osdu.core.aws.cognito.AWSCognitoClient;
import org.opengroup.osdu.core.aws.logging.AmazonLogConfig;
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

    public static void main(String[] args) {
        EnvironmentVariables environmentVariables = new EnvironmentVariables();

        // System print lines go to cloudwatch automatically within ECS
        System.out.println("Running Queue processor with the following environment variables:");
        System.out.println(String.format("Region: %s", environmentVariables.region));
        System.out.println(String.format("Max Batch Request Count: %s", environmentVariables.maxBatchRequestCount));
        System.out.println(String.format("Queue Name: %s", environmentVariables.queueName));
        System.out.println(String.format("Dead letter queue name: %s", environmentVariables.deadLetterQueueName));
        System.out.println(String.format("Max index threads: %s", environmentVariables.maxIndexThreads));
        System.out.println(String.format("Max messages allowed: %s", environmentVariables.maxMessagesAllowed));
        System.out.println(String.format("Target url: %s", environmentVariables.targetURL));
        System.out.println(String.format("Keep alive time in min: %s", environmentVariables.keepAliveTimeInMin));

        try {
            System.out.println("Starting Indexer Queue and obtaining Arguments");

            System.out.println("Retrieving indexer service account JWT");
            AWSCognitoClient cognitoClient = new AWSCognitoClient(environmentVariables.cognitoClientId, environmentVariables.cognitoAuthFlow,
                    environmentVariables.cognitoUser, environmentVariables.cognitoPassword);
            String indexerServiceAccountJWT = cognitoClient.getToken();
            if(indexerServiceAccountJWT == null){
                System.out.println("Indexer service account not set up correctly");
            }

            System.out.println(String.format("Connecting to the SQS Queue: %s", environmentVariables.queueName));
            AmazonSQSConfig sqsConfig = new AmazonSQSConfig(environmentVariables.region);
            AmazonSQS sqsClient = sqsConfig.AmazonSQS();

            System.out.println(String.format("Creating a thread pool with %s threads", environmentVariables.maxIndexThreads));
            ThreadPoolExecutor executorPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(environmentVariables.maxIndexThreads);

            final String deadLetterQueueUrl = sqsClient.getQueueUrl(environmentVariables.deadLetterQueueName).getQueueUrl();
            System.out.println(String.format("Dead letter queue url: %s", deadLetterQueueUrl));
            List<Message> messages = IndexerQueueService.getMessages(sqsClient, environmentVariables.queueName, environmentVariables.maxBatchRequestCount, environmentVariables.maxMessagesAllowed);
            System.out.println(String.format("Processing %s messages from storage queue", messages.size()));

            String indexerUrl = String.format("http://%s/%s",environmentVariables.targetURL, "api/indexer/v2/_dps/task-handlers/index-worker" );

            if (!messages.isEmpty()) {

                List<IndexProcessor> indexProcessors = IndexerQueueService.processQueue(messages,  indexerUrl, executorPool, indexerServiceAccountJWT);
                System.out.println(String.format("%s Messages Processed", indexProcessors.size()));

                List<IndexProcessor> failedProcessors =  indexProcessors.stream().filter(indexProcessor -> indexProcessor.result == CallableResult.Fail || indexProcessor.exception != null).collect(Collectors.toList());
                System.out.println(String.format("%s Messages Failed", failedProcessors.size()));

                List<SendMessageResult> deadLetterResults = IndexerQueueService.sendMsgsToDeadLetterQueue(deadLetterQueueUrl, failedProcessors, sqsClient);
                System.out.println(String.format("%s Messages Dead Lettered", deadLetterResults.size()));

                List<DeleteMessageBatchRequestEntry> deleteEntries = indexProcessors.stream().map(indexProcessor -> new DeleteMessageBatchRequestEntry(indexProcessor.messageId, indexProcessor.receiptHandle)).collect(Collectors.toList());
                System.out.println(String.format("%s Messages Deleting", deleteEntries.size()));

                final String sqsQueueUrl = sqsClient.getQueueUrl(environmentVariables.queueName).getQueueUrl();
                List<DeleteMessageBatchRequest> deleteBatchRequests = IndexerQueueService.createMultipleBatchDeleteRequest(sqsQueueUrl, deleteEntries, environmentVariables.maxBatchRequestCount);
                System.out.println(String.format("%s Delete Batch Request Created", deleteBatchRequests.size()));

                List<DeleteMessageBatchResult> deleteMessageBatchResults = IndexerQueueService.deleteMessages(deleteBatchRequests, sqsClient);
                System.out.println(String.format("%s Requests Deleted", deleteMessageBatchResults.size()));
            }

        } catch (ExecutionException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        }catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            System.out.println("Finished.");
        }
    }
}
