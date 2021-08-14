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

import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;

public class EnvironmentVariables {
    public String region;
    public String queueUrl;
    public String targetURL;
    public String deadLetterQueueUrl;
    public EnvironmentVariables() {
        this.region = System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1";
        this.targetURL =System.getenv("AWS_INDEXER_INDEX_API");
        String environment = System.getenv("ENVIRONMENT");
        K8sLocalParameterProvider provider = new K8sLocalParameterProvider();
        this.queueUrl = provider.getPropertyAsStringOrDefault("storage-sqs-url",System.getenv("AWS_STORAGE_QUEUE_URL") );
        this.deadLetterQueueUrl = provider.getPropertyAsStringOrDefault("indexer-deadletter-queue-sqs-url", System.getenv("AWS_DEADLETTER_QUEUE_URL"));
    }
}
