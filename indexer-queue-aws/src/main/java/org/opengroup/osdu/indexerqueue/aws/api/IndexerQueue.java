/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amazonaws.services.sqs.model.*;
import org.opengroup.osdu.core.aws.sqs.AmazonSQSConfig;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;

import java.util.List;
import java.util.concurrent.ExecutionException;

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

    private static final JaxRsDpsLog logger = LogProvider.getLogger();
    private static final String ALL_MESSAGES_ATTRIBUTES = "All";
    private final EnvironmentVariables environmentVariables;
    private final String queueUrl;

    IndexerQueue() {
        environmentVariables = new EnvironmentVariables();
        String targetUrl = environmentVariables.getTargetURL();
        queueUrl = environmentVariables.getQueueUrl();
        // System print lines go to cloudwatch automatically within ECS
        logger.info("Running Queue processor with the following environment variables:");
        logger.info(String.format("Region: %s", environmentVariables.getRegion()));
        logger.info(String.format("Queue Name: %s", queueUrl));
        logger.info(String.format("Dead letter queue name: %s", environmentVariables.getDeadLetterQueueUrl()));
        logger.info(String.format("Target url: %s", targetUrl));
        logger.info(String.format("Connecting to the SQS Queue: %s", queueUrl));
    }

    private void run() throws InterruptedException {
        try (IndexerQueueService service = new IndexerQueueService(environmentVariables, this::getSqsClient)) {
            int maxMessages = environmentVariables.getMaxAllowedMessages();
            AmazonSQS sqsClient = getSqsClient();
            boolean shouldLoop = true;
            
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
            receiveMessageRequest.setMaxNumberOfMessages(maxMessages);
            receiveMessageRequest.withMessageAttributeNames(ALL_MESSAGES_ATTRIBUTES);
            receiveMessageRequest.withAttributeNames(ALL_MESSAGES_ATTRIBUTES);
            receiveMessageRequest.setWaitTimeSeconds(environmentVariables.getMaxWaitTime());
            
            while (shouldLoop) {
                try {
                    if (service.getNumMessages() < maxMessages) {
                        List<Message> retrievedMessages = sqsClient.receiveMessage(receiveMessageRequest).getMessages();
                        service.putMessages(retrievedMessages);
                    } else {
                        Thread.sleep(10000);
                    }
                    
                    if (service.isUnhealthy()) {
                        logger.error("Service is unhealthy. Halting.");
                        shouldLoop = false;
                    }
                } catch (InterruptedException e) {
                    shouldLoop = false;
                    logger.error("Interrupted while waiting.", e);
                }
            }
        } finally {
            logger.error("Service state change to unhealthy while processing storage messages. Terminating pod.");
        }
        // Done to ensure that the IndexerQueue exits with non-zero status code
        System.exit(2);
    }
    
    public static void main(String[] args) throws InterruptedException {
        IndexerQueue queue = new IndexerQueue();
        queue.run();
    }

    private AmazonSQS getSqsClient() {
        AmazonSQSConfig sqsConfig = new AmazonSQSConfig(environmentVariables.getRegion());
        return sqsConfig.AmazonSQS();
    }
}


