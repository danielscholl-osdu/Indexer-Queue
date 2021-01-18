/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexerqueue.reference.util;

import com.google.gson.Gson;
import lombok.extern.java.Log;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.reference.config.IndexerQueueConfigProperties;
import org.opengroup.osdu.indexerqueue.reference.messagebus.IMessageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Log
@Component
public class TaskBuilder {

  private final Gson gson = new Gson();
  private final IMessageFactory mq;
  private final IndexerQueueConfigProperties configurationProperties;

  @Autowired
  public TaskBuilder(IMessageFactory mq, IndexerQueueConfigProperties configurationProperties) {
    this.configurationProperties = configurationProperties;
    this.mq = mq;
  }

  public HttpStatus createTask(RecordChangedMessages recordChangedMessages, DpsHeaders headers) {
    log.info(String
        .format("project-id: %s | location: %s | queue-id: %s | indexer-host: %s | message: %s",
            configurationProperties.getGoogleCloudProject(),
            configurationProperties.getDeploymentLocation(),
            IMessageFactory.DEFAULT_QUEUE_NAME,
            configurationProperties.getIndexerHost(),
            recordChangedMessages));
    return sendMessage(gson.toJson(recordChangedMessages));
  }

  public HttpStatus createTask(CloudTaskRequest request) {
    return sendMessage(request.getMessage());
  }

  private HttpStatus sendMessage(String message) {
    try {
      mq.sendMessage(IMessageFactory.DEFAULT_QUEUE_NAME, message);
    } catch (Exception e) {
      log.info(e.getMessage());
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return HttpStatus.OK;
  }
}
