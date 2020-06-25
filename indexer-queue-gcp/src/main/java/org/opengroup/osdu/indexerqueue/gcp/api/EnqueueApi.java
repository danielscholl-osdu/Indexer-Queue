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

package org.opengroup.osdu.indexerqueue.gcp.api;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.extern.java.Log;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.gcp.util.HeadersInfo;
import org.opengroup.osdu.indexerqueue.gcp.util.AppEngineTaskBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Log
@RestController
@RequestMapping("/_ah/push-handlers")
public class EnqueueApi {

    private final Gson gson = new Gson();

    @Autowired
    private HttpServletRequest request;
    @Autowired
    private HeadersInfo headersInfo;
    @Autowired
    private AppEngineTaskBuilder appEngineTaskBuilder;

    // THIS IS AN INTERNAL USE API ONLY
    // THAT MEANS WE DON'T DOCUMENT IT IN SWAGGER, ACCESS IS LIMITED TO ADMIN ROLE
    @PostMapping("/enqueue")
    public ResponseEntity enqueueTask() throws IOException {
        RecordChangedMessages message = this.getTaskQueueMessage();
        this.headersInfo.getHeaders().getHeaders().put(DpsHeaders.ACCOUNT_ID, message.getDataPartitionId());
        this.headersInfo.getHeaders().put(DpsHeaders.DATA_PARTITION_ID, message.getDataPartitionId());
        if (message.hasCorrelationId()) {
            this.headersInfo.getHeaders().put(DpsHeaders.CORRELATION_ID, message.getCorrelationId());
        }
        log.info(String.format("message headers: %s", this.headersInfo.toString()));

        CloudTaskRequest request = CloudTaskRequest.builder().message(this.gson.toJson(message)).url(Constants.WORKER_RELATIVE_URL).build();
        this.appEngineTaskBuilder.createTask(request);

        return new ResponseEntity(org.springframework.http.HttpStatus.OK);
    }

    private RecordChangedMessages getTaskQueueMessage() {
        try {
            JsonParser jsonParser = new JsonParser();
            String requestBody = this.request.getReader().lines().collect(Collectors.joining("\n"));
            JsonElement jsonRoot = jsonParser.parse(requestBody);
            JsonElement message = jsonRoot.getAsJsonObject().get("message");
            if (message == null) {
                throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid record change message", "message object not found", "'message' object not found in PubSub message");
            }

            RecordChangedMessages recordChangedMessages = this.gson.fromJson(message.toString(), RecordChangedMessages.class);
            String payload = recordChangedMessages.getData();
            if(Strings.isNullOrEmpty(payload)) {
                throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid record change message", "message data not found", "'message.data' not found in PubSub message");
            }

            String decodedPayload = new String(Base64.getDecoder().decode(payload));
            recordChangedMessages.setData(decodedPayload);

            Map<String, String> attributes = recordChangedMessages.getAttributes();
            if (attributes == null || attributes.size() == 0) {
                throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid record change message", "attribute map not found", String.format("PubSub message: %s", recordChangedMessages));
            }
            Map<String, String> lowerCase = new HashMap<>();
            attributes.forEach((key, value) -> lowerCase.put(key.toLowerCase(), value));
            recordChangedMessages.setAttributes(lowerCase);
            if (recordChangedMessages.missingAccountId()) {
                throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid tenant", "tenant-id missing", String.format("PubSub message: %s", recordChangedMessages));
            }
            return recordChangedMessages;
        } catch (IOException e) {
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Request payload parsing error", "Unable to parse request payload.", e);
        }
    }
}
