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



import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.xray.model.Http;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import com.amazonaws.services.sqs.model.Message;


public class IndexProcessor implements Callable<IndexProcessor> {
    public CallableResult result;
    public Exception exception;
    public Message message;
    public String messageId;
    public String receiptHandle;
    public StringBuilder response;
    public String targetURL;
    public String indexerServiceAccountJWT;

    public IndexProcessor(Message message, String targetUrl, String indexServiceAccountJWT){
        this.message = message;
        this.targetURL = targetUrl;
        this.receiptHandle = message.getReceiptHandle();
        result = CallableResult.Pass;
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

            HttpURLConnection connection = getConnection(body, convertedMessage.attributes.get("data-partition-id"));

            sendRequest(connection, body);

            getResponse(connection);
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
            result = CallableResult.Fail;
            exception = e;
        } catch (ProtocolException e) {
            System.out.println(e.getMessage());
            result = CallableResult.Fail;
            exception = e;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            result = CallableResult.Fail;
            exception = e;
        } catch (Exception e){
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

        Map<String, MessageAttributeValue> messageAttributes = message.getMessageAttributes();
        MessageAttributeValue dataPartitionIdValue = messageAttributes.get("data-partition-id");
        MessageAttributeValue accountIdValue = messageAttributes.get("account-id");

        Map<String, String> attributes = new HashMap<>();
        attributes.put("data-partition-id", dataPartitionIdValue.getStringValue());
        attributes.put("account-id", accountIdValue.getStringValue());

        convertedMessage.attributes = attributes;
        return convertedMessage;
    }

    private HttpURLConnection getConnection(String body, String dataPartitionId) throws IOException {
        URL url = new URL(this.targetURL);
        System.out.println(String.format("The url is: %s", url));
        HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Length",
                Integer.toString(body.getBytes().length));;
        connection.setRequestProperty("Content-Type",
                "application/json");
        connection.setRequestProperty("data-partition-id", dataPartitionId);
        connection.setRequestProperty("Authorization", "Bearer " + this.indexerServiceAccountJWT);

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
}
