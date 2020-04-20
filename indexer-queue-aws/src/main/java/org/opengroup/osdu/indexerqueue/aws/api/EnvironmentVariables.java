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

public class EnvironmentVariables {
    public String region;
    public String queueName;
    public String targetURL;
    public String deadLetterQueueName;
    public int maxBatchRequestCount;
    public int maxMessagesAllowed;
    public int maxIndexThreads;
    public long keepAliveTimeInMin;

    public EnvironmentVariables() {

        this.region = System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1";
        this.queueName = System.getenv("AWS_QUEUE_INDEXER_NAME")  != null ? System.getenv("AWS_QUEUE_INDEXER_NAME") : "dev-osdu-storage-queue";
        this.targetURL = System.getenv("AWS_INDEXER_INDEX_API") != null ? System.getenv("AWS_INDEXER_INDEX_API"): "ECSALB-os-indexer-355262993.us-east-1.elb.amazonaws.com/api/indexer/v2/_dps/task-handlers/index-worker";
        this.deadLetterQueueName = System.getenv("AWS_DEADLETTER_QUEUE_NAME") != null ? System.getenv("AWS_DEADLETTER_QUEUE_NAME") : "dev-osdu-storage-dead-letter-queue";
        this.maxIndexThreads = System.getenv("MAX_INDEX_THREADS") != null ? Integer.parseInt(System.getenv("MAX_INDEX_THREADS")) : 50;
        this.maxBatchRequestCount = System.getenv("MAX_REQUEST_COUNT") != null ? Integer.parseInt(System.getenv("MAX_REQUEST_COUNT")) : 10;
        this.maxMessagesAllowed = System.getenv("MAX_MESSAGE_COUNT") != null ? Integer.parseInt(System.getenv("MAX_MESSAGE_COUNT")) : 100000;
        this.keepAliveTimeInMin = System.getenv("KEEP_ALIVE_IN_MINUTES") != null ? Long.parseLong(System.getenv("KEEP_ALIVE_IN_MINUTES")) : 9999;
    };
}
