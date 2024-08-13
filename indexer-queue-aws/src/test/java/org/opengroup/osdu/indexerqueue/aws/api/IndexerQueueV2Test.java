/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexerqueue.aws.api;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.opengroup.osdu.core.aws.sqs.AmazonSQSConfig;
import uk.org.webcompere.systemstubs.rules.SystemExitRule;
import uk.org.webcompere.systemstubs.security.AbortExecutionException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class IndexerQueueV2Test {

    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @Captor
    private ArgumentCaptor<ReceiveMessageRequest> receiveRequest;

    @Rule
    public SystemExitRule exitRule = new SystemExitRule();

    @Before
    public void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @Test
    public void test_usesQueueUrlV2() throws InterruptedException {
        AmazonSQS mockedSqs = Mockito.mock(AmazonSQS.class);
        ReceiveMessageResult receiveResult = Mockito.mock(ReceiveMessageResult.class);
        List<Message> messages = new ArrayList<Message>();
        int maxMessages = 12;
        int maxWaitTime = 5;
        String queueRegion = "someAwsRegion";
        String queueUrl = "someSQSQueueURL";
        try (MockedConstruction<EnvironmentVariables> envMock = Mockito.mockConstruction(EnvironmentVariables.class, (mock, context) -> {
            when(mock.getMaxWaitTime()).thenReturn(maxWaitTime);
            when(mock.getRegion()).thenReturn(queueRegion);
            when(mock.getMaxAllowedMessages()).thenReturn(maxMessages);
            when(mock.getQueueUrlV2()).thenReturn(queueUrl);
        })) {
            try (MockedConstruction<AmazonSQSConfig> queueMock = Mockito.mockConstruction(AmazonSQSConfig.class, (mock, context) -> {
                when(mock.AmazonSQS()).thenReturn(mockedSqs);
            })) {
                try (MockedConstruction<IndexerQueueService> serviceMock = Mockito.mockConstruction(IndexerQueueService.class, (mock, context) -> {
                    when(mock.isUnhealthy()).thenReturn(false).thenReturn(false).thenReturn(true);
                    doNothing().when(mock).putMessages(messages);
                    when(mock.getNumMessages()).thenReturn(maxMessages / 2).thenReturn(maxMessages * 2).thenReturn(maxMessages).thenAnswer(new Answer<Integer>() {
                        @Override
                        public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                            fail("Should not get to this point. Indicates unhealthy check is not validated!");
                            return maxMessages;
                        }
                    });
                })) {
                    when(mockedSqs.receiveMessage(any(ReceiveMessageRequest.class))).thenAnswer(new Answer<ReceiveMessageResult>() {
                        @Override
                        public ReceiveMessageResult answer(InvocationOnMock invocationOnMock) throws Throwable {
                            return receiveResult;
                        }
                    });
                    when(receiveResult.getMessages()).thenReturn(messages);
                    IndexerQueueV2 indexerQueueV2 = new IndexerQueueV2();
                    assertThrows(AbortExecutionException.class, indexerQueueV2::run);
                    assertNotEquals(0, (long) exitRule.getExitCode());

                    verify(mockedSqs, times(1)).receiveMessage(receiveRequest.capture());
                    ReceiveMessageRequest request = receiveRequest.getValue();
                    assertEquals(queueUrl, request.getQueueUrl());
                    assertEquals(maxMessages, request.getMaxNumberOfMessages().intValue());
                    assertEquals(maxWaitTime, request.getWaitTimeSeconds().intValue());
                    assertTrue(request.getMessageAttributeNames().contains("All"));
                    assertTrue(request.getAttributeNames().contains("All"));
                    envMock.constructed().forEach(envMockConstructed ->
                        verify(envMockConstructed, never()).getQueueUrl()
                    );
                }
            }

        }
    }
}
