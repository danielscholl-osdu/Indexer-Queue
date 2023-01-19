// Copyright © Microsoft Corporation
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//      http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexerqueue.azure.queue;

import com.google.gson.Gson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.azure.di.AzureBootstrapConfig;
import org.opengroup.osdu.indexerqueue.azure.exceptions.IndexerRetryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opengroup.osdu.indexerqueue.azure.exceptions.ValidStorageRecordNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

import static java.lang.String.format;

/***
 * A class to send recordChangedMessages to indexer-service.
 */
@Component
public class RecordChangedMessageHandler implements IRecordChangedMessageHandler {

  @Autowired
  private AzureBootstrapConfig azureBootstrapConfig;

  private final Gson gson = new Gson();
  private HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
  private Logger logger = LoggerFactory.getLogger(RecordChangedMessageHandler.class.getName());

  /***
   * Create an Http Request to index-worker endpoint of indexer service and deliver RecordChangedMessage.
   * In case of failure, exponential backoff retry is done based on current delivery count by service bus.
   * @param recordChangedMessage 1 batch of PubSubInfo messages generated by storage service and delivered by service bus.
   * @return
   */
  public void sendMessagesToIndexer(RecordChangedMessages recordChangedMessage) {

    try (CloseableHttpClient indexWorkerClient = httpClientBuilder.build()) {
      logger.debug("Sending recordChangedMessages to indexer service {}: ", this.gson.toJson(recordChangedMessage));
      HttpPost indexWorkerRequest = new HttpPost(azureBootstrapConfig.getIndexerWorkerURL());
      indexWorkerRequest.setEntity(new StringEntity(this.gson.toJson(recordChangedMessage)));
      indexWorkerRequest.setHeader(DpsHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

      Map<String, String> att = recordChangedMessage.getAttributes();

      indexWorkerRequest.setHeader(DpsHeaders.DATA_PARTITION_ID, att.get(DpsHeaders.DATA_PARTITION_ID));
      indexWorkerRequest.setHeader(DpsHeaders.CORRELATION_ID, att.get(DpsHeaders.CORRELATION_ID));

      CloseableHttpResponse response = indexWorkerClient.execute(indexWorkerRequest);

      if(response.getStatusLine().getStatusCode() == 404){
        throw new ValidStorageRecordNotFoundException(format("Indexer unable to proceed, valid storage record not found. Response status: %d", response.getStatusLine().getStatusCode()));
      }
      else if (response.getStatusLine().getStatusCode() > 299) {
        throw new IndexerRetryException(format("Failed to send message %s to Indexer. Response status: %d", recordChangedMessage.getData(), response.getStatusLine().getStatusCode()));
      }
    } catch (IOException e) {
      String errorMessage = "Exception occurs during sending message to the Indexer:" + e.getMessage();
      throw new IndexerRetryException(errorMessage);
    }
  }
}
