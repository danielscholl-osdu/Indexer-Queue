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

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import org.junit.*;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;
import org.mockito.Mock;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.mock;
import org.junit.contrib.java.lang.system.EnvironmentVariables;


public class IndexerQueueTest {

    private static final int MIN_TIMEOUT = 5000;
    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public Timeout timeout = new Timeout(MIN_TIMEOUT) {
        public Statement apply(Statement base, Description description) {
            return new FailOnTimeout(base, MIN_TIMEOUT) {
                @Override
                public void evaluate() throws Throwable {
                    try {
                        super.evaluate();
                        throw new TimeoutException();
                    } catch (Exception e) {}
                }
            };
        }
    };

    @Mock
    private AmazonSQS sqsClient = mock(AmazonSQS.class);

    private final String queueUrl ="localhost";

    @Before
    public void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @After
    public void tearDown() {
        System.setOut(standardOut);
    }

    @Test
    public void test_processIndexMessages_EmptyMessage() throws ExecutionException, InterruptedException {

        ThreadPoolExecutor executorPool = mock(ThreadPoolExecutor.class);

        List<Message> messages = new ArrayList<Message>();

        IndexerQueue.processIndexMessages(messages, "indexerUrl", queueUrl, "deadLetterQueueUrl", executorPool, sqsClient);

        Assert.assertTrue(outputStreamCaptor.toString().trim().contains("0 Requests Deleted"));
    }

    @Test
    public void test_processIndexMessages_ValidMessage() throws ExecutionException, InterruptedException {

        ThreadPoolExecutor executorPool = mock(ThreadPoolExecutor.class);

        List<Message> messages = new ArrayList<Message>();

        Message msg = new Message();
        msg.addMessageAttributesEntry("authorization", new MessageAttributeValue());

        messages.add(msg);

        IndexerQueue.processIndexMessages(messages, "indexerUrl", queueUrl, "deadLetterQueueUrl", executorPool, sqsClient);

        Assert.assertTrue(outputStreamCaptor.toString().trim().contains("1 Requests Deleted"));
    }

    @Test
    public void test_processReIndexMessages_EmptyMessage() throws ExecutionException, InterruptedException {

        ThreadPoolExecutor executorPool = mock(ThreadPoolExecutor.class);

        List<Message> messages = new ArrayList<Message>();

        IndexerQueue.processReIndexMessages(messages, "reIndexerUrl", queueUrl, executorPool, sqsClient);

        Assert.assertTrue(outputStreamCaptor.toString().trim().contains("0 Requests Deleted"));
    }

    @Test(expected = TestTimedOutException.class)
    public void test_Main() throws K8sParameterNotFoundException {


        IndexerQueue.main(new String[0]);

    }

}

