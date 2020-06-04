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

import org.opengroup.osdu.core.aws.ssm.ParameterStorePropertySource;
import org.opengroup.osdu.core.aws.ssm.SSMConfig;

public class EnvironmentVariables {
    public String region;
    public String queueUrl;
    public String targetURL;
    public String deadLetterQueueUrl;
    private String prefix = "osdu";

    private ParameterStorePropertySource ssm;

    public EnvironmentVariables() {

        Boolean ssmEnabled = System.getenv("SSM_ENABLED") != null ? Boolean.parseBoolean(System.getenv("SSM_ENABLED")) : false;
        this.region = System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1";
            String environment =  ssmEnabled ? System.getenv("ENVIRONMENT") : "";
            String parameterPrefix = String.format("/%s/%s", prefix, environment);
            SSMConfig ssmConfig = new SSMConfig();
            ssm = ssmConfig.amazonSSM();
            this.queueUrl = ssmEnabled ? ssm.getProperty(String.format("%s/%s", parameterPrefix, "storage/storage-sqs-url")).toString() : System.getenv("AWS_STORAGE_QUEUE_URL")  != null ? System.getenv("AWS_STORAGE_QUEUE_URL") : "dev-osdu-storage-queue";
            this.targetURL = System.getenv("AWS_INDEXER_INDEX_API") != null ? System.getenv("AWS_INDEXER_INDEX_API"): "ECSALB-os-indexer-355262993.us-east-1.elb.amazonaws.com/api/indexer/v2/_dps/task-handlers/index-worker";
        this.deadLetterQueueUrl = ssmEnabled ? ssm.getProperty(String.format("%s/%s", parameterPrefix, "indexer-queue/indexer-deadletter-queue-sqs-url")).toString() :System.getenv("AWS_DEADLETTER_QUEUE_URL") != null ? System.getenv("AWS_DEADLETTER_QUEUE_URL") : "https://sqs.us-east-1.amazonaws.com/888733619319/dev-osdu-storage-dead-letter-queue";
    }
}
