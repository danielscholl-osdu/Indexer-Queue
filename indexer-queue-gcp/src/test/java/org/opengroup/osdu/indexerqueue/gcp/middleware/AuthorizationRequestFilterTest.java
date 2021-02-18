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
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.gcp.util.HeadersInfo;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringRunner.class)
@PrepareForTest({HttpServletRequest.class})
public class AuthorizationRequestFilterTest {
    private String CLOUD_TRACE_CONTEXT = "x-cloud-trace-context";
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private FilterChain filterChain;
    @Mock
    DpsHeaders dpsHeaders;
    @Mock
    private HeadersInfo headersInfo;
    @InjectMocks
    private AuthorizationRequestFilter filter;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void shouldNot_addAnyHeaders_filterSwaggerPath() throws IOException, ServletException {
        when(this.httpServletRequest.getRequestURI()).thenReturn("push-handlers");
        when(this.headersInfo.getHeaders()).thenReturn(dpsHeaders);
        when(dpsHeaders.getCorrelationId()).thenReturn("correlation-id");
        Map<String, String> headers = new HashMap<>();

        when(this.httpServletRequest.getMethod()).thenReturn("POST");

        this.filter.doFilter(this.httpServletRequest, this.httpServletResponse, this.filterChain);
        assertFalse(headers.containsKey(DpsHeaders.CORRELATION_ID));
        assertFalse(headers.containsKey(CLOUD_TRACE_CONTEXT));
    }

    @Test
    public void should_throw_forbidden_given_emptyHeaders() throws IOException, ServletException {
        when(this.httpServletRequest.getRequestURI()).thenReturn("task-handlers");
        when(this.httpServletRequest.getMethod()).thenReturn("POST");
        try {
            this.filter.doFilter(this.httpServletRequest, this.httpServletResponse, this.filterChain);
            fail("should throw");
        } catch (AppException e) {
            assertEquals(403, e.getError().getCode());
            assertEquals("The user or service is not authorized to perform this action", e.getError().getMessage());
        }
    }

    @Test
    public void should_throw_forbidden_missingAuthHeader() throws IOException, ServletException {
        when(this.httpServletRequest.getRequestURI()).thenReturn("task-handlers");

        List<String> headerList = new ArrayList<>();
        headerList.add("tenant1");
        Enumeration<String> enumeration = Collections.enumeration(headerList);

        when(this.httpServletRequest.getMethod()).thenReturn("POST");
        when(this.httpServletRequest.getHeaderNames()).thenReturn(enumeration);
        when(this.httpServletRequest.getHeader(DpsHeaders.ACCOUNT_ID)).thenReturn("tenant1");

        try {
            this.filter.doFilter(this.httpServletRequest, this.httpServletResponse, this.filterChain);
            fail("should throw");
        } catch (AppException e) {
            assertEquals(403, e.getError().getCode());
            assertEquals("Empty authorization header", e.getError().getMessage());
        }
    }

    @Test
    public void should_throw_forbidden_invalidAuthHeader() throws IOException, ServletException {
        when(this.httpServletRequest.getRequestURI()).thenReturn("task-handlers");

        List<String> headerList = new ArrayList<>();
        headerList.add(DpsHeaders.ACCOUNT_ID);
        headerList.add(DpsHeaders.AUTHORIZATION);
        Enumeration<String> enumeration = Collections.enumeration(headerList);

        when(this.httpServletRequest.getMethod()).thenReturn("POST");
        when(this.httpServletRequest.getHeaderNames()).thenReturn(enumeration);
        when(this.httpServletRequest.getHeader(DpsHeaders.ACCOUNT_ID)).thenReturn("tenant1");
        when(this.httpServletRequest.getHeader(DpsHeaders.AUTHORIZATION)).thenReturn("geer.fereferv.cefe=");

        try {
            this.filter.doFilter(this.httpServletRequest, this.httpServletResponse, this.filterChain);
            fail("should throw");
        } catch (AppException e) {
            assertEquals(403, e.getError().getCode());
            assertEquals("Invalid authorization header", e.getError().getMessage());
        }
    }
}
