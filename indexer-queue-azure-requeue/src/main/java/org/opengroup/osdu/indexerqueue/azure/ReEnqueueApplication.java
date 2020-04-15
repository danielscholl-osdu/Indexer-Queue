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

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Re Enqueue Azure Functions with HTTP Trigger.
 * Indexer Service -> ReEnqueue Function -> Service Bus
 */
public class ReEnqueueApplication {

    @Autowired
    TopicClient topicClient;
    @Autowired
    Message message;

    @FunctionName("re-enqueue")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage request,
            ExecutionContext context) throws ServiceBusException, InterruptedException {

        //TODO: this should be moved to Azure client-lib
        final String INDEXER_QUEUE_KEY = "x-functions-key";
        final String PATH_TASK_HANDLERS = "re-enqueue";

        if (request == null || request.getBody() == null || request.getHeaders() == null){
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid request", "request object not found");
        }
        // Only indexer should be able to call the re-enqueue endpoint
        if (request.getHeaders().get(INDEXER_QUEUE_KEY) == null ||
                !request.getHeaders().get(INDEXER_QUEUE_KEY).equals(System.getenv("INDEXER_QUEUE_KEY"))) {
            throw new AppException(HttpStatus.SC_UNAUTHORIZED, "UnAuthorized", "Cannot call this endpoint");
        }

        context.getLogger().info("ReEnqueue Request: " + request.getBody().toString());
        context.getLogger().info("ReEnqueue Headers: " + request.getHeaders().toString());

        String uri = request.getUri().toString().toLowerCase();

        if (request.getHttpMethod().equals(HttpMethod.POST)) {

            if (uri.contains(PATH_TASK_HANDLERS)) {
                checkApiAccess(request);
            }
        }
        final String CONTENT_TYPE = "application/json";
        final String topicName = System.getenv("TOPIC_NAME");
        final String connectionString = System.getenv("SERVICE_BUS");

        topicClient = new TopicClient(new ConnectionStringBuilder(connectionString, topicName));
        message = new Message(request.getBody().toString());
        message.setContentType(CONTENT_TYPE);

        try {
            topicClient.send(message);
        } catch (AppException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Could not send to SB.", "/nProblem sending request to SB: \" + e.getMessage()");
        }
        return request.createResponseBuilder(com.microsoft.azure.functions.HttpStatus.OK)
                .header("Content-Type", "application/json")
                .build();
    }

    private void checkApiAccess(HttpRequestMessage request) {

        try {
            Map<String, String> requestHeaders = getHeaders(request);
            if (requestHeaders == null || requestHeaders.isEmpty()) {
                throw new AppException(HttpStatus.SC_FORBIDDEN, "Access denied", "The user or service is not authorized to perform this action", "Request headers are either null or empty");
            }

            if (!requestHeaders.containsKey(DpsHeaders.AUTHORIZATION) && !requestHeaders.containsKey("Authorization")) {
                throw new AppException(HttpStatus.SC_FORBIDDEN, "Access denied", "Empty authorization header");
            }

//           TODO: Verify authorization token sent by Index Worker
//            Similar to indexer-queue-boot-gcp/.../middleware/AuthorizationRequestFilter.java

        } catch (Exception e) {
            throw new AppException(HttpStatus.SC_FORBIDDEN, "Access denied", "The user or service is not authorized to perform this action", e);
        }
    }

    private Map<String, String> getHeaders(HttpRequestMessage request) {
        return (Map<String, String>) request.getHeaders();
    }
}
