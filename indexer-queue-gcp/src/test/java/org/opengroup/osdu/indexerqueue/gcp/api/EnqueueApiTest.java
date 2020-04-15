// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.indexerqueue.gcp.api;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.http.HeadersUtil;
import org.opengroup.osdu.indexerqueue.gcp.util.AppEngineTaskBuilder;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


@RunWith(SpringRunner.class)
@PrepareForTest({HeadersUtil.class, DpsHeaders.class})
public class EnqueueApiTest {

    private final String requestBodyEmpty = "{}";
    private final String requestBodyValid = "{\"message\":{\"data\":\"W3tcImlkXCI6XCJ0ZW5hbnQxOndlbGxkYjp3ZWxsYm9yZS1kOTAzM2FlMS1mYjE1LTQ5NmMtOWJhMC04ODBmZDFkMmIyY2ZcIixcImtpbmRcIjpcInRlbmFudDE6d2VsbGRiOndlbGxib3JlOjEuMC4wXCIsXCJvcFwiOlwiY3JlYXRlXCJ9XQ==\",\"attributes\":{\"account-id\":\"tenant1\",\"slb-correlation-id\":\"b5a281bd-f59d-4db2-9939-b2d85036fc7e\"},\"messageId\":\"75328163778221\", \"publishTime\":\"2018-05-08T21:48:56.131Z\"}}";
    private final String requestBodyMissingCorrelationId = "{\"message\":{\"data\":\"W3tcImlkXCI6XCJ0ZW5hbnQxOndlbGxkYjp3ZWxsYm9yZS1kOTAzM2FlMS1mYjE1LTQ5NmMtOWJhMC04ODBmZDFkMmIyY2ZcIixcImtpbmRcIjpcInRlbmFudDE6d2VsbGRiOndlbGxib3JlOjEuMC4wXCIsXCJvcFwiOlwiY3JlYXRlXCJ9XQ==\",\"attributes\":{\"account-id\":\"tenant1\"},\"messageId\":\"75328163778221\",\"publishTime\":\"2018-05-08T21:48:56.131Z\"}}";
    private final String requestBodyMissingTenantId = "{\"message\":{\"messageId\":\"testId1\",\"data\":\"W3tcImlkXCI6XCJ0ZW5hbnQxOndlbGxkYjp3ZWxsYm9yZS1kOTAzM2FlMS1mYjE1LTQ5NmMtOWJhMC04ODBmZDFkMmIyY2ZcIixcImtpbmRcIjpcInRlbmFudDE6d2VsbGRiOndlbGxib3JlOjEuMC4wXCIsXCJvcFwiOlwiY3JlYXRlXCJ9XQ==\", \"attributes\":{\"slb-correlation-id\":\"b5a281bd-f59d-4db2-9939-b2d85036fc7e\"}}}";

    private final String ACCOUNT_ID = "any-account";

    @Mock
    private HttpServletRequest request;
    @Mock
    private DpsHeaders dpsHeaders;
    @Mock
    private BufferedReader bufferReader;
    @Mock
    private AppEngineTaskBuilder appEngineTaskBuilder;
    @InjectMocks
    private EnqueueApi sut;

    @Before
    public void setup() throws IOException {
        initMocks(this);

        when(request.getReader()).thenReturn(bufferReader);

        Map<String, String> headers = new HashMap<>();
        headers.put(DpsHeaders.ACCOUNT_ID, this.ACCOUNT_ID);
        when(this.dpsHeaders.getHeaders()).thenReturn(headers);
    }

    @Test
    public void should_return200_when_given_validRequest_enqueueTaskTest() {
        should_return200_enqueueTaskTest(requestBodyValid);
    }

    @Test
    public void should_return200AndSevereLog_when_missing_correlationId_enqueueTaskTest() {
        should_return200_enqueueTaskTest(requestBodyMissingCorrelationId);
    }

    @Test
    public void should_return400_when_missing_tenantIdInRequestBody_enqueueTaskTest() {
        should_return400_enqueueTaskTest(requestBodyMissingTenantId, "tenant-id missing");
    }

    @Test
    public void should_return400_when_given_emptyRequestBody_enqueueTaskTest() {
        should_return400_enqueueTaskTest(requestBodyEmpty, "message object not found");
    }

    private void should_return200_enqueueTaskTest(String requestBody) {
        createRequestStream(requestBody);
        ResponseEntity response = this.sut.enqueueTask();
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode().value());
    }

    private void should_return400_enqueueTaskTest(String requestBody, String errorMessage) {
        try {
            createRequestStream(requestBody);
            this.sut.enqueueTask();
            fail("Should throw exception");
        } catch (AppException e) {
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getError().getCode());
            Assert.assertEquals(errorMessage, e.getError().getMessage());
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    private void createRequestStream(String requestBody) {
        String[] requestBodies = {requestBody};
        Stream<String> stringStream = Stream.of(requestBodies);
        when(bufferReader.lines()).thenReturn(stringStream);
    }
}
