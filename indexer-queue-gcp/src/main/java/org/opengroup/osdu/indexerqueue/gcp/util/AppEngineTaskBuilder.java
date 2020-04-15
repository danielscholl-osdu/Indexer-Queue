// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.indexerqueue.gcp.util;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import lombok.extern.java.Log;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.appengine.api.taskqueue.RetryOptions.Builder.withTaskRetryLimit;

@Log
@Component
public class AppEngineTaskBuilder {

    private static final RetryOptions RETRY_OPTIONS = withTaskRetryLimit(5).taskAgeLimitSeconds(86400).minBackoffSeconds(120).maxBackoffSeconds(3600).maxDoublings(5);

    @Autowired
    private IndexerQueueIdentifier indexerQueueProvider;
    @Autowired
    private DpsHeaders dpsHeaders;

    public void createTask(CloudTaskRequest request) {
//        log.info(String.format("message: %s", request.getMessage()));
//        log.info("CREATE TASK Headers: " + headersInfo.toString());
//        log.info("INDEXER QUEUE NAME: " + indexerQueueProvider.getQueueId());

        Queue queue = QueueFactory.getQueue(indexerQueueProvider.getQueueId());
        queue.add(TaskOptions.Builder.withUrl(request.getUrl()).payload(request.getMessage()).headers(this.dpsHeaders.getHeaders()).retryOptions(RETRY_OPTIONS));

        log.info("queued task successfully");
    }
}