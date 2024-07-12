/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexerqueue.aws.api;

import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EnvironmentVariables {
    private final String region;
    private final String queueUrl;
    private final String queueUrlV2;
    private final String targetURL;
    private final String deadLetterQueueUrl;
    private final int maxAllowedMessages;
    private final int maxIndexThreads;
    private final int maxWaitTime;
    private final int fullWorkerWaitTime;
    private final int maxBatchRequestCount;
    private final int maxIndexTime;
    private final int maxWorkerThreadWaitTime;
    private final Properties appProperties;
    private static final JaxRsDpsLog logger = LogProvider.getLogger();

    public EnvironmentVariables() {
        this.region = System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1";
        this.targetURL = System.getenv("AWS_INDEXER_INDEX_API");
        K8sLocalParameterProvider provider = new K8sLocalParameterProvider();
        this.queueUrl = provider.getParameterAsStringOrDefault("storage-sqs-url", System.getenv("AWS_STORAGE_QUEUE_URL"));
        this.queueUrlV2 = provider.getParameterAsStringOrDefault("storage-v2-sqs-url", System.getenv("AWS_STORAGE_V2_QUEUE_URL"));
        this.deadLetterQueueUrl = provider.getParameterAsStringOrDefault("indexer-deadletter-queue-sqs-url", System.getenv("AWS_DEADLETTER_QUEUE_URL"));
        appProperties = new Properties();
        try {
            try (InputStream inputStream = EnvironmentVariables.class.getClassLoader().getResourceAsStream("application.properties")) {
                appProperties.load(inputStream);
            }
        } catch (IOException | NullPointerException e) {
            logger.error("Could not load properties file.", e);
        }
        this.maxAllowedMessages = getPropertyOrDefault("MAX_RETRIEVED_MESSAGES", 10);
        this.maxIndexThreads = getPropertyOrDefault("MAX_INDEX_THREADS", 1000);
        this.maxWaitTime = getPropertyOrDefault("MAX_WAIT_TIME", 10);
        this.maxBatchRequestCount = getPropertyOrDefault("MAX_DELETED_MESSAGES", 10);
        this.fullWorkerWaitTime = getPropertyOrDefault("FULL_WORKER_WAIT_TIME", 10);
        this.maxIndexTime = getPropertyOrDefault("MAX_INDEX_TIME", 60);
        this.maxWorkerThreadWaitTime = getPropertyOrDefault("MAX_WORKER_WAIT_TIME", 10);
        logger.info(String.format("Max retrieved messages: %s", this.maxAllowedMessages));
        logger.info(String.format("Max indexing threads: %s", this.maxIndexThreads));
        logger.info(String.format("Max time to wait when polling the SQS queue: %s", this.maxWaitTime));
        logger.info(String.format("Max deleted messages: %s", this.maxBatchRequestCount));
        logger.info(String.format("Wait time (in milliseconds) when internal buffer is full: %s", this.fullWorkerWaitTime));
        logger.info(String.format("Max indexing time allowed: %s", this.maxIndexTime));
        logger.info(String.format("Max worker thread waiting time: %s", this.maxWorkerThreadWaitTime));
    }

    private int getPropertyOrDefault(String property, int defaultValue) {
        String value = System.getenv(property);
        if (value == null || value.isEmpty()) {
            value = appProperties.getProperty(property);
        }
        int retValue = defaultValue;
        if (value != null) {
            try {
                retValue = Integer.parseInt(value);
            } catch (NumberFormatException | NullPointerException e) {
                logger.error(String.format("Could not parse number of property %s with value %s", property, value), e);
            }
        } else {
            logger.info(String.format("Property %s does not have a value set. Using %d as default.", property, defaultValue));
        }
        return retValue;
    }

    public String getRegion() {
        return this.region;
    }

    public String getQueueUrl() {
        return this.queueUrl;
    }

    public String getQueueUrlV2() {
        return this.queueUrlV2;
    }

    public String getTargetURL() {
        return this.targetURL;
    }

    public String getDeadLetterQueueUrl() {
        return this.deadLetterQueueUrl;
    }

    public int getMaxAllowedMessages() {
        return this.maxAllowedMessages;
    }

    public int getMaxBatchRequestCount() {
        return this.maxBatchRequestCount;
    }

    public int getMaxIndexThreads() {
        return this.maxIndexThreads;
    }

    public int getMaxWaitTime() {
        return this.maxWaitTime;
    }

    public int getFullWorkerWaitTime() {
        return this.fullWorkerWaitTime;
    }

    public int getMaxIndexTime() {
        return this.maxIndexTime;
    }

    public int getMaxWorkerThreadWaitTime() {
        return this.maxWorkerThreadWaitTime;
    }
}
