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
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class IndexerQueueServiceTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private AmazonSQS sqsClient = mock(AmazonSQS.class);

    private ReceiveMessageResult receiveResult;

    private String queueUrl ="localhost";

    @Before
    public void setUp() {
        receiveResult = new ReceiveMessageResult();
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


}
