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


import com.amazonaws.services.sqs.model.Message;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NewIndexProcessorTest {

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final String invalidUrl = "targetUrl_invalid";
    private final String localhostUrl = "https://localhost";
    private final String StreamString = "This\ris\ra\rstring\r";

    Message message = new Message();
    @InjectMocks
    NewIndexProcessor processor = new NewIndexProcessor(message, invalidUrl, "indexServiceAccountJWT");

    @Before
    public void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
        this.processor.setMessage(message);
    }

    @After
    public void tearDown() {
        System.setOut(standardOut);
    }

    @Test
    public void test_Call_badTargetUrl() {


        this.processor.setTargetURL(invalidUrl);
        this.processor.setResult(CallableResult.Pass);

        NewIndexProcessor result = (NewIndexProcessor) processor.call();

        Assert.assertEquals(processor, result);
        Assert.assertTrue(processor.expectionExists());
    }

    @Test
    public void test_Call_localhost() {

        this.processor.setTargetURL(localhostUrl);
        this.processor.setResult(CallableResult.Pass);

        NewIndexProcessor result = (NewIndexProcessor) processor.call();

        Assert.assertEquals(processor, result);
        Assert.assertTrue(processor.expectionExists());

    }

    @Test
    public void test_Call_0ByteStream() throws IOException {

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        when(mockConnection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(mockConnection.getInputStream()).thenReturn(mock(InputStream.class));

        try (MockedConstruction<URL> url = Mockito.mockConstruction(URL.class, (mockUrl, context) -> {
            when(mockUrl.openConnection()).thenReturn(mockConnection);
        })) {

            try (MockedConstruction<RecordChangedMessages> msgs = Mockito.mockConstruction(RecordChangedMessages.class, (mockMsgs, context) -> {
                when(mockMsgs.getMessageId()).thenReturn("messageId");
            })) {
                this.processor.setTargetURL(localhostUrl);
                this.processor.setResult(CallableResult.Pass);

                message.setBody("body");


                NewIndexProcessor result = (NewIndexProcessor) processor.call();

                Assert.assertEquals(processor, result);
                Assert.assertTrue(processor.expectionExists());
            }
        }
    }

    @Test
    public void test_Call() throws IOException {

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        when(mockConnection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(StreamString.getBytes()));

        try (MockedConstruction<URL> url = Mockito.mockConstruction(URL.class, (mockUrl, context) -> {
            when(mockUrl.openConnection()).thenReturn(mockConnection);
        })) {

            try (MockedConstruction<RecordChangedMessages> msgs = Mockito.mockConstruction(RecordChangedMessages.class, (mockMsgs, context) -> {
                when(mockMsgs.getMessageId()).thenReturn("messageId");
            })) {
                this.processor.setResponse(new StringBuilder());
                this.processor.setTargetURL(localhostUrl);
                this.processor.setResult(CallableResult.Pass);

                message.setBody("body");


                NewIndexProcessor result = (NewIndexProcessor) processor.call();

                Assert.assertEquals(processor, result);
                Assert.assertEquals(StreamString, result.getResponse().toString());
                Assert.assertFalse(processor.expectionExists());
            }
        }
    }

    @Test
    public void test_Get(){
        NewIndexProcessor processor2 = new NewIndexProcessor(new Message(), "targetUrl", "indexServiceAccountJWT");
        processor2.setResponse(new StringBuilder());
        Assert.assertEquals(CallableResult.Pass, processor2.getResult());
        Assert.assertNull(processor2.getReceiptHandle());
    }
}
