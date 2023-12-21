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



import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.amazonaws.services.sqs.model.Message;


public class IndexProcessor implements Callable<IndexProcessor> {
    private CallableResult result;
    private Exception exception;
    private Message message;
    private String messageId;
    private String receiptHandle;
    private StringBuilder response = new StringBuilder();
    private String targetURL;
    private String indexerServiceAccountJWT;

    public IndexProcessor(Message message, String targetUrl, String indexServiceAccountJWT){
        this.message = message;
        this.targetURL = targetUrl;
        this.receiptHandle = message.getReceiptHandle();
        result = CallableResult.Pass;
        this.indexerServiceAccountJWT = indexServiceAccountJWT;
    }

    public IndexProcessor(Message message, String targetUrl, String indexServiceAccountJWT, CallableResult result){
        this.message = message;
        this.targetURL = targetUrl;
        this.receiptHandle = message.getReceiptHandle();
        this.result = result;
        this.indexerServiceAccountJWT = indexServiceAccountJWT;
    }

    @Override
    public IndexProcessor call() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            this.messageId = message.getMessageId();
            System.out.println(String.format("Processing message: %s", this.messageId));

            RecordChangedMessages convertedMessage = getConvertedMessage(message);
            String body = mapper.writeValueAsString(convertedMessage);

            HttpURLConnection connection = getConnection(body, convertedMessage.attributes);

            sendRequest(connection, body);

            getResponse(connection);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            result = CallableResult.Fail;
            exception = e;
        }
        System.out.println(result);
        if(result == CallableResult.Fail) {
            System.out.println(exception.getMessage());
        }
        return this;
    }

    private RecordChangedMessages getConvertedMessage(Message message){
        RecordChangedMessages convertedMessage = new RecordChangedMessages();
        convertedMessage.data = message.getBody();
        convertedMessage.messageId = message.getMessageId();

        Map<String, String> messageAttributes = message
                .getMessageAttributes()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        attribute -> attribute
                                .getValue()
                                .getStringValue()));
        convertedMessage.attributes = messageAttributes;

        return convertedMessage;
    }

    private HttpURLConnection getConnection(String body, Map<String, String> attributes) throws IOException {
        URL url = new URL(this.targetURL);
        System.out.println(String.format("The url is: %s", url));

        HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("data-partition-id", attributes.get("data-partition-id"));
        connection.setRequestProperty("Authorization", this.indexerServiceAccountJWT);
        connection.setRequestProperty("user", attributes.get("user"));
        connection.setRequestProperty("Content-Length", Integer.toString(body.getBytes().length));
        connection.setRequestProperty("x-user-id", attributes.get("user"));
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        return connection;
    }

    private void sendRequest(HttpURLConnection connection, String body) throws IOException {
        DataOutputStream wr = new DataOutputStream (
                connection.getOutputStream());
        wr.writeBytes(body);
        wr.close();
    }

    private void getResponse(HttpURLConnection connection) throws IOException {
        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = rd.readLine()) != null) {
            this.response.append(line);
            this.response.append('\r');
        }
        rd.close();
    }

    public boolean expectionExists(){
        return exception != null;
    }

    public CallableResult getResult() {
        return this.result;
    }

    public Exception getException() {
        return this.exception;
    }

    public Message getMessage() {
        return this.message;
    }

    public String getMessageId() {
        return this.messageId;
    }

    public String getReceiptHandle() {
        return this.receiptHandle;
    }

    public StringBuilder getResponse() {
        return this.response;
    }

    /*public String getTargetURL() {
        return this.targetURL;
    }*/

    public void setResult(CallableResult newResult) {
        this.result = newResult;
    }

    public void setException(Exception newException) {
        this.exception = newException;
    }

    public void setMessage(Message newMessage) {
        this.message = newMessage;
    }

    public void setMessageId(String newMessageId) {
        this.messageId = newMessageId;
    }

    /*public void setReceiptHandle(String newReceiptHandle) {
        this.receiptHandle = newReceiptHandle;
    }*/

    public void setResponse(StringBuilder newResponse) {
        this.response = newResponse;
    }

    public void setTargetURL(String newTargetURL) {
        this.targetURL = newTargetURL;
    }
}
