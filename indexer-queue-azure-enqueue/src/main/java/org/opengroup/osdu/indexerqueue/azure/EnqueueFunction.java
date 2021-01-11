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

package org.opengroup.osdu.indexerqueue.azure;

import com.google.gson.Gson;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusTopicTrigger;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexerqueue.azure.util.SBMessageBuilder;
import org.opengroup.osdu.indexerqueue.azure.util.MDCContextMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.Map;

/**
 * Azure Functions with Service Topic Trigger. It will be invoked when a new
 * message is received at the Service Bus Topic.
 */
public class EnqueueFunction {

  @Autowired
  HttpPost indexWorkerRequest;
  @Autowired
  SBMessageBuilder sbMessageBuilder;
  @Autowired
  RecordChangedMessages recordChangedMessage;

  private final Gson gson = new Gson();
  private HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
  private final static Logger LOGGER = LoggerFactory.getLogger(EnqueueFunction.class);
  private MDCContextMap mdcContextMap = new MDCContextMap();
  private String log_prefix = "queue";

  public EnqueueFunction() {
  }

  @FunctionName("enqueue")
  public Response run(
      @ServiceBusTopicTrigger(name = "message", topicName = "%TOPIC_NAME%", subscriptionName = "%SUBSCRIPTION_NAME%", connection = "SERVICE_BUS") String serviceBusRequest,
      @BindingName("MessageId") String messageId, @BindingName("EnqueuedTimeUtc") String enqueuedTimeUtc,
      @BindingName("DeliveryCount") String deliveryCount, final ExecutionContext context) throws Exception {

    // TODO: this should be moved to Azure client-lib
    final String INDEXER_QUEUE_KEY = "x-functions-key";
    sbMessageBuilder = new SBMessageBuilder();
    recordChangedMessage = sbMessageBuilder.getServiceBusMessage(serviceBusRequest);

    String correlationId = recordChangedMessage.getCorrelationId();
    String dataPartitionId = recordChangedMessage.getDataPartitionId();
    MDC.setContextMap(mdcContextMap.getContextMap(correlationId, dataPartitionId));
    LOGGER.info("{} {} {}", new Object[]{log_prefix, "serviceBusRequest: ", serviceBusRequest});

    recordChangedMessage.setMessageId(messageId);
    recordChangedMessage.setPublishTime(enqueuedTimeUtc);
    int currentTry = Integer.parseInt(deliveryCount);

    try (CloseableHttpClient indexWorkerClient = httpClientBuilder.build()) {
      LOGGER.info("{} {} {}", new Object[]{log_prefix,"INDEXER_WORKER_URL: ", System.getenv("INDEXER_WORKER_URL")});
      LOGGER.info("{} {} {}", new Object[]{log_prefix,"recordChangedMessage: ", this.gson.toJson(recordChangedMessage)});
      indexWorkerRequest = new HttpPost(System.getenv("INDEXER_WORKER_URL"));
      indexWorkerRequest.setEntity(new StringEntity(this.gson.toJson(recordChangedMessage)));
      indexWorkerRequest.setHeader(INDEXER_QUEUE_KEY, System.getenv("INDEXER_QUEUE_KEY"));
      indexWorkerRequest.setHeader(DpsHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

      Map<String, String> att = recordChangedMessage.getAttributes();

      indexWorkerRequest.setHeader(DpsHeaders.DATA_PARTITION_ID, att.get(DpsHeaders.DATA_PARTITION_ID));
      indexWorkerRequest.setHeader(DpsHeaders.CORRELATION_ID, att.get(DpsHeaders.CORRELATION_ID));
      indexWorkerClient.execute(indexWorkerRequest);
    } catch (Exception e) {
      // exponential backoff retry based on current delivery count by SB
      long waitTime = ((long) Math.pow(2, currentTry) * 10L);
      java.sql.Timestamp before = new java.sql.Timestamp(new Date().getTime());
      Thread.sleep(waitTime);
      java.sql.Timestamp after = new java.sql.Timestamp(new Date().getTime());
      LOGGER.info("{} {} {}", new Object[]{log_prefix,"wait time: ", (after.getTime() - before.getTime())});

      throw new Exception("Error creating request to index worker: " + e.getMessage());
    }

    MDC.clear();
    return Response.status(HttpStatus.SC_OK).type(String.valueOf(MediaType.APPLICATION_JSON)).build();

    /*
     * return Response.status(HttpStatus.SC_OK) .build();
     */
  }
}