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


import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class IndexerQueueServiceTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private AmazonSQS sqsClient = mock(AmazonSQS.class);

    private ReceiveMessageResult receiveResult;

    private SendMessageResult sendMessageResult;

    private final String queueUrl ="localhost";

    @Before
    public void setUp() {

        receiveResult = new ReceiveMessageResult();
        sendMessageResult = new SendMessageResult();

    }

    @Test
    public void test_getMessages_10Messages_pass() {
        // Arrange
        int numOfmessages = 10;
        int maxMessageCount = 10;


        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        receiveMessageRequest.setMaxNumberOfMessages(numOfmessages);
        receiveMessageRequest.withMessageAttributeNames("All");
        receiveMessageRequest.withAttributeNames("All");
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Message msg = new Message();
            messages.add(msg);
        }

        receiveResult.setMessages(messages);

        Mockito.when(sqsClient.receiveMessage(receiveMessageRequest)).thenReturn(receiveResult);

        // Act
         List<Message> actualMessages = IndexerQueueService.getMessages(sqsClient, queueUrl, numOfmessages, maxMessageCount);

        // Assert
        assertEquals(messages.get(1), actualMessages.get(1));
        assertEquals(messages.size(), actualMessages.size());
    }

    @Test
    public void test_getMessages_0Messages_pass() {
        // Arrange
        int numOfmessages = 10;
        int maxMessageCount = 10;


        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        receiveMessageRequest.setMaxNumberOfMessages(numOfmessages);
        receiveMessageRequest.withMessageAttributeNames("All");
        receiveMessageRequest.withAttributeNames("All");

        List<Message> messages = new ArrayList<>();

        receiveResult.setMessages(messages);

        Mockito.when(sqsClient.receiveMessage(receiveMessageRequest)).thenReturn(receiveResult);

        // Act
        List<Message> actualMessages = IndexerQueueService.getMessages(sqsClient, queueUrl, numOfmessages, maxMessageCount);

        // Assert
        assertEquals(messages.size(), actualMessages.size());
    }


    @Test
    public void test_sendMsgsToDeadLetterQueue(){

        // Arrange

        List<IndexProcessor> indexProcessors = new ArrayList<IndexProcessor>();

        Message message = mock(Message.class);
        when(message.getBody()).thenReturn("body");
        when(message.getMessageAttributes()).thenReturn(new HashMap<String, MessageAttributeValue>());

        IndexProcessor indexProcessor = new IndexProcessor(message, "targetUrl", "indexServiceAccountJWT");
        indexProcessor.messageId = "messageId";
        indexProcessors.add(indexProcessor);

        Mockito.when(sqsClient.sendMessage(any())).thenReturn(sendMessageResult);

        //Exception not exist
        IndexerQueueService.sendMsgsToDeadLetterQueue(queueUrl, indexProcessors, sqsClient);

        //Exception exist
        indexProcessor.exception = new Exception("Exception");
        IndexerQueueService.sendMsgsToDeadLetterQueue(queueUrl, indexProcessors, sqsClient);

        // Assert
        verify(sqsClient, times(2)).sendMessage(any());

    }








    @Test
    public void test_ChangeMessageVisibilityTimeout() {
      // Arrange

        List<IndexProcessor> indexProcessors = new ArrayList<IndexProcessor>();

        Message message = mock(Message.class);
        when(message.getReceiptHandle()).thenReturn("ReceiptHandle");
        when(message.getAttributes()).thenReturn(new HashMap<String, String>());

        IndexProcessor indexProcessor = new IndexProcessor(message, "targetUrl", "indexServiceAccountJWT");
        indexProcessor.messageId = "messageId";
        indexProcessors.add(indexProcessor);
        when(sqsClient.changeMessageVisibilityBatch(any(), any())).thenReturn(null);

        // Act
        IndexerQueueService.ChangeMessageVisibilityTimeout(sqsClient, queueUrl, indexProcessors);

        // Assert
        verify(sqsClient, times(1)).changeMessageVisibilityBatch(any(), any());
    }



    @Test
    public void test_ChangeMessageVisibilityTimeout_max10() {
        // Arrange

        List<IndexProcessor> indexProcessors = new ArrayList<IndexProcessor>();

        for(int i = 1; i < 10; i++){
            Message msg = mock(Message.class);
            Map<String, String> map = new HashMap<String, String>();
            map.put("ApproximateReceiveCount", String.valueOf(i));
            when(msg.getReceiptHandle()).thenReturn("ReceiptHandle");
            when(msg.getAttributes()).thenReturn(map);
            IndexProcessor indexProcessor = new IndexProcessor(msg, "targetUrl", "indexServiceAccountJWT");
            indexProcessor.messageId = "messageId";
            indexProcessors.add(indexProcessor);
        }

        Message message19 = mock(Message.class);
        Map<String, String> map19 = new HashMap<String, String>();
        map19.put("ApproximateReceiveCount", "19");
        when(message19.getReceiptHandle()).thenReturn("ReceiptHandle");
        when(message19.getAttributes()).thenReturn(map19);
        IndexProcessor indexProcessor9 = new IndexProcessor(message19, "targetUrl", "indexServiceAccountJWT");
        indexProcessor9.messageId = "messageId";
        indexProcessors.add(indexProcessor9);

        when(sqsClient.changeMessageVisibilityBatch(any(), any())).thenReturn(null);

        // Act
        IndexerQueueService.ChangeMessageVisibilityTimeout(sqsClient, queueUrl, indexProcessors);

        // Assert
        verify(sqsClient, times(1)).changeMessageVisibilityBatch(any(), any());
    }



    @Test(expected = NullPointerException.class)
    public void test_getMessages_invalidqueuename_fail() {
        // Arrange
        int numOfmessages = 10;
        int maxMessageCount = 10;

        String invalidQueueName = "invalid";

        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        receiveMessageRequest.setMaxNumberOfMessages(numOfmessages);
        receiveMessageRequest.withMessageAttributeNames("data-partition-id", "account-id");

        List<Message> messages = new ArrayList<>();

        receiveResult.setMessages(messages);

        Mockito.when(sqsClient.receiveMessage(receiveMessageRequest)).thenReturn(receiveResult);

        // Act
        List<Message> actualMessages = IndexerQueueService.getMessages(sqsClient, invalidQueueName, numOfmessages, maxMessageCount);
    }

    @Test
    public void test_createIndexCompletableFutures(){
        //Arrange
        ThreadPoolExecutor executorPool = mock(ThreadPoolExecutor.class);

        List<Message> messages = new ArrayList<Message>();
        Message msg = new Message();
        Map<String, MessageAttributeValue> attributes = new HashMap<String, MessageAttributeValue>();
        attributes.put("authorization", new MessageAttributeValue());
        msg.setMessageAttributes(attributes);
        messages.add(msg);
        //Act
        List<CompletableFuture<IndexProcessor>> list = IndexerQueueService.createIndexCompletableFutures(messages, executorPool, "url");

        assertEquals(list.size(), 1);
    }

    @Test
    public void test_createReIndexCompletableFutures(){
        //Arrange
        ThreadPoolExecutor executorPool = mock(ThreadPoolExecutor.class);

        List<Message> messages = new ArrayList<Message>();
        Message msg = new Message();
        Map<String, MessageAttributeValue> attributes = new HashMap<String, MessageAttributeValue>();
        attributes.put("authorization", new MessageAttributeValue());
        msg.setMessageAttributes(attributes);
        messages.add(msg);
        //Act
        List<CompletableFuture<ReIndexProcessor>> list = IndexerQueueService.createReIndexCompletableFutures(messages, executorPool, "url");

        assertEquals(list.size(), 1);
    }

}
