/* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public abstract class IndexProcessor implements Callable<IndexProcessor> {

    protected CallableResult result;
    protected Exception exception;
    protected Message message;
    protected String messageId;
    protected String receiptHandle;
    protected StringBuilder response = new StringBuilder();
    protected String targetURL;
    protected String indexerServiceAccountJWT;
    protected final ObjectMapper mapper = new ObjectMapper();
    private static final JaxRsDpsLog logger = LogProvider.getLogger();

    protected IndexProcessor(Message message, String targetUrl, String indexServiceAccountJWT){
        this.message = message;
        this.targetURL = targetUrl;
        this.receiptHandle = message.getReceiptHandle();
        result = CallableResult.PASS;
        this.indexerServiceAccountJWT = indexServiceAccountJWT;
    }

    protected IndexProcessor(Message message, String targetUrl, String indexServiceAccountJWT, CallableResult result){
        this.message = message;
        this.targetURL = targetUrl;
        this.receiptHandle = message.getReceiptHandle();
        this.result = result;
        this.indexerServiceAccountJWT = indexServiceAccountJWT;
    }

    protected abstract String getType();

    protected HttpURLConnection getConnection(String body, Map<String, String> attributes) throws IOException {
        URL url = new URL(this.targetURL);
        logger.info(String.format("The url is: %s", url));

        HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Length", Integer.toString(body.getBytes().length));
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("data-partition-id", attributes.get("data-partition-id"));
        connection.setRequestProperty("Authorization", this.indexerServiceAccountJWT);
        connection.setRequestProperty("user", attributes.get("user"));
        connection.setRequestProperty("x-user-id", attributes.get("user"));
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        return connection;
    }
    public boolean expectionExists(){
        return exception != null;
    }

    private String readInputStream(InputStream is) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder resultBuilder = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            resultBuilder.append(line);
            resultBuilder.append('\r');
        }
        rd.close();
        return resultBuilder.toString();
    }
    protected void getResponse(HttpURLConnection connection) throws IOException {
        try {
            logger.info(String.format("Response code for message %s is %d", this.messageId, connection.getResponseCode()));
            this.response.append(readInputStream(connection.getInputStream()));
        } catch (FileNotFoundException e) {
            logger.info(String.format("Response error for message %s is %s", this.messageId, readInputStream(connection.getErrorStream())));
            throw e;
        }
    }

    protected void sendRequest(HttpURLConnection connection, String body) throws IOException {
        DataOutputStream wr = new DataOutputStream (
            connection.getOutputStream());
        wr.writeBytes(body);
        wr.close();
    }

    protected abstract String getBody(Message message, Map<String, String> attributes) throws JsonProcessingException;

    private Map<String, String> getMessageAttributes(Message message) {
        return message
            .getMessageAttributes()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                                      attribute -> attribute
                                          .getValue()
                                          .getStringValue()));
    }

    @Override
    public IndexProcessor call() {
        try {
            this.messageId = message.getMessageId();
            logger.info(String.format("Processing message: %s with type: %s", this.messageId, this.getType()));

            Map<String, String> attributes = getMessageAttributes(message);
            String body = getBody(message, attributes);

            HttpURLConnection connection = getConnection(body, attributes);

            sendRequest(connection, body);

            getResponse(connection);
        } catch (Exception e) {
            logger.info(e.getMessage());
            result = CallableResult.FAIL;
            logger.error(String.format("Could not send %s message", this.getType()), e);
            exception = e;
        }
        logger.info(result.toString());
        if(result == CallableResult.FAIL) {
            logger.info(exception.getMessage());
        }
        return this;
    }
    public CallableResult getResult() {
        return this.result;
    }

    public String getReceiptHandle() {
        return this.receiptHandle;
    }

    public String getMessageId() {
        return this.messageId;
    }

    public Message getMessage() {
        return this.message;
    }

    public Exception getException() {
        return this.exception;
    }

    public StringBuilder getResponse() {
        return this.response;
    }

    public void setResult(CallableResult newResult) {
        this.result = newResult;
    }

    public void setMessage(Message newMessage) {
        this.message = newMessage;
    }

    public void setResponse(StringBuilder newResponse) {
        this.response = newResponse;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public void setTargetURL(String newTargetURL) {
        this.targetURL = newTargetURL;
    }

}
