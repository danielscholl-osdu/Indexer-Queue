/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.indexerqueue.aws.api;



import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.*;

import com.amazonaws.services.sqs.model.Message;


public class NewIndexProcessor extends IndexProcessor {

    public NewIndexProcessor(Message message, String targetUrl, String indexServiceAccountJWT){
        super(message, targetUrl, indexServiceAccountJWT);
    }

    public NewIndexProcessor(Message message, String targetUrl, String indexServiceAccountJWT, CallableResult result){
        super(message, targetUrl, indexServiceAccountJWT, result);
    }

    public void setMessageId(String id) {
        this.messageId = id;
    }

    @Override
    protected String getType() {
        return "New Index Processor";
    }

    @Override
    protected String getBody(Message message, Map<String, String> attributes) throws JsonProcessingException {
        RecordChangedMessages convertedMessage = new RecordChangedMessages();
        convertedMessage.data = message.getBody();
        convertedMessage.messageId = message.getMessageId();

        convertedMessage.attributes = attributes;
        return mapper.writeValueAsString(convertedMessage);
    }
}
