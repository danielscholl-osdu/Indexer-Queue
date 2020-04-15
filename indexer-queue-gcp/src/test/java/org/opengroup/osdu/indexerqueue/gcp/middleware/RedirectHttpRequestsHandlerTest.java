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

package org.opengroup.osdu.indexerqueue.gcp.middleware;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.gcp.model.AppEngineHeaders;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class RedirectHttpRequestsHandlerTest {
    @Mock
    private DpsHeaders dpsHeaders;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private FilterChain filterChain;
    @Mock
    private Map<String, String> headers;
    @InjectMocks
    private RedirectHttpRequestsHandler sut;

    @Before
    public void setup() {
        headers = new HashMap<>();
    }

    @Test
    public void should_throwAppException302WithHttpsLocation_when_isNotATaskQueue_And_IsNotUsingHttps() throws IOException, ServletException {
        when(this.httpServletRequest.isSecure()).thenReturn(false);
        try {
            sut.doFilter(httpServletRequest, httpServletResponse, filterChain);
            fail("should throw");
        } catch (AppException e) {
            assertEquals(302, e.getError().getCode());
        }
    }

    @Test
    public void should_throwAppException302WithHttpsLocation_when_using_notIndexerTaskQueue() throws IOException, ServletException {
        headers.put(AppEngineHeaders.TASK_QUEUE_NAME, "poller-queue");
        when(httpServletRequest.isSecure()).thenReturn(false);

        try {
            sut.doFilter(httpServletRequest, httpServletResponse, filterChain);
            fail("should throw");
        } catch (AppException e) {
            assertEquals(302, e.getError().getCode());
        }
    }

    @Test
    public void should_notThrowAppException302WithHttpsLocation_when_isAHttpsRequest() throws IOException, ServletException {
        headers.put(AppEngineHeaders.TASK_QUEUE_NAME, "dummyTenant");
        when(dpsHeaders.getHeaders()).thenReturn(headers);
        when(httpServletRequest.isSecure()).thenReturn(true);
        sut.doFilter(httpServletRequest, httpServletResponse, filterChain);
    }

    @Test
    public void should_notThrowAppException302WithHttpsLocation_when_isATaskQueue() throws IOException, ServletException {
        headers.put(AppEngineHeaders.TASK_QUEUE_NAME, "dummyTenant-os-indexer-queue");
        when(httpServletRequest.isSecure()).thenReturn(true);
        sut.doFilter(httpServletRequest, httpServletResponse, filterChain);
    }
}
