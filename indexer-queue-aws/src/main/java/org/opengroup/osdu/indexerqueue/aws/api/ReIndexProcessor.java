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

import com.amazonaws.services.sqs.model.Message;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class ReIndexProcessor implements Callable<ReIndexProcessor> {
    public CallableResult result;
    public Exception exception;
    public Message message;
    public String messageId;
    public String receiptHandle;
    public StringBuilder response;
    public String targetURL;
    public String indexerServiceAccountJWT;
    public ReIndexProcessor(Message message, String targetUrl, String indexServiceAccountJWT){
        this.message = message;
        this.targetURL = targetUrl;
        this.receiptHandle = message.getReceiptHandle();
        result = CallableResult.Pass;
        this.indexerServiceAccountJWT = indexServiceAccountJWT;
    }

    @Override
    public ReIndexProcessor call() {
        try {
            this.messageId = message.getMessageId();
            System.out.printf("Processing message: %s %n", this.messageId);

            String body = message.getBody();

            Map<String, String> messageAttributes = message
                    .getMessageAttributes()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            attribute -> attribute
                                    .getValue()
                                    .getStringValue()));
            HttpURLConnection connection = getConnection(body, messageAttributes);

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

    public boolean expectionExists(){
        return exception != null;
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

    private void sendRequest(HttpURLConnection connection, String body) throws IOException {
        DataOutputStream wr = new DataOutputStream (
                connection.getOutputStream());
        wr.writeBytes(body);
        wr.close();
    }
    private HttpURLConnection getConnection(String body, Map<String, String> attributes) throws IOException {
        URL url = new URL(this.targetURL);
        System.out.printf("The url is: %s %n", url);
        HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Length",
                Integer.toString(body.getBytes().length));
        connection.setRequestProperty("Content-Type",
                "application/json");
        connection.setRequestProperty("data-partition-id", attributes.get("data-partition-id"));
        connection.setRequestProperty("Authorization", this.indexerServiceAccountJWT);
        connection.setRequestProperty("user", attributes.get("user"));
        connection.setRequestProperty("x-user-id", attributes.get("user"));
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        return connection;
    }

}
