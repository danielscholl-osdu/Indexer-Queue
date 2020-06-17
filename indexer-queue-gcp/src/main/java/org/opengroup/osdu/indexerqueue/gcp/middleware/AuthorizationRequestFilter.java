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

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.http.ResponseHeaders;
import org.opengroup.osdu.core.gcp.util.HeadersInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

@Component
public class AuthorizationRequestFilter implements Filter {

    private static final String PATH_PUSH_HANDLERS = "push-handlers";
    private static final String PATH_TASK_HANDLERS = "task-handlers";

    private final JsonFactory JSON_FACTORY = new JacksonFactory();
    private FilterConfig filterConfig;

    @Autowired
    private HeadersInfo headersInfo;

    @Value("${GOOGLE_AUDIENCES}")
    private String GOOGLE_AUDIENCES;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI().toLowerCase();

        if (httpRequest.getMethod().equals(HttpMethods.POST)) {
            //TODO: Find a way to check if this endpoint is called from pubsub only
//            if (uri.contains(PATH_PUSH_HANDLERS)) return;
            if (uri.contains(PATH_TASK_HANDLERS)) {
                checkApiAccess(httpRequest);
            }
        }
        chain.doFilter(request, response);
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Map<String, List<Object>> standardHeaders = ResponseHeaders.STANDARD_RESPONSE_HEADERS;
        for (Map.Entry<String, List<Object>> header : standardHeaders.entrySet()) {
            httpResponse.addHeader(header.getKey(), header.getValue().toString());
        }
        if (httpResponse.getHeader(DpsHeaders.CORRELATION_ID) == null) {
            httpResponse.addHeader(DpsHeaders.CORRELATION_ID, headersInfo.getHeaders().getCorrelationId());
        }
        httpResponse.setContentType("application/json");
    }

    public void init(final FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    public void destroy() {
        this.filterConfig = null;
    }

    // only accept traffic from google service account JWT with aud specified
    // by ENTITLEMENT_TARGET_AUDIENCE and reject traffic from any other client
    private void checkApiAccess(HttpServletRequest request) {

        try {
            if (request == null || request.getHeaderNames() == null) {
                throw new AppException(HttpStatus.SC_FORBIDDEN, "Access denied", "The user or service is not authorized to perform this action", "Request headers are either null or empty");
            }
            Map<String, String> requestHeaders = getHeaders(request);

            if (!requestHeaders.containsKey("Authorization") && !requestHeaders.containsKey("authorization")) {
                throw new AppException(HttpStatus.SC_FORBIDDEN, "Access denied", "Empty authorization header");
            }

            String authHdr = requestHeaders.get(DpsHeaders.AUTHORIZATION);
            if (authHdr == null) {
                authHdr = requestHeaders.get("Authorization");
            }

            String[] bearerIdTokenParts = authHdr.split(" ");
            if (bearerIdTokenParts.length != 2) {
                throw new AppException(HttpStatus.SC_FORBIDDEN, "Access denied", "Invalid authorization header", "Invalid authorization header");
            }

            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            String[] targetAudience = {GOOGLE_AUDIENCES};

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(httpTransport, JSON_FACTORY)
                    .setIssuer("https://accounts.google.com")
                    .setAudience(Arrays.asList(targetAudience))
                    .build();

            GoogleIdToken idToken = verifier.verify(bearerIdTokenParts[1]);
            if (idToken != null) {
                boolean expVerificationResult = idToken.verifyExpirationTime(System.currentTimeMillis(), 60);
                if (!expVerificationResult) {
                    throw new AppException(HttpStatus.SC_FORBIDDEN, "Access denied", "The user or service is not authorized to perform this action", "Authorization token is expired");
                }
                String email = idToken.getPayload().getEmail();
                if (!email.startsWith("datafier@")) {
                    throw new AppException(HttpStatus.SC_FORBIDDEN, "Access denied", "The user or service is not authorized to perform this action", "Authorization token has invalid email claim");
                }
            } else {
                throw new AppException(HttpStatus.SC_FORBIDDEN, "Access denied", "The user or service is not authorized to perform this action", "Google cannot verify authorization token");
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new AppException(HttpStatus.SC_FORBIDDEN, "Access denied", "The user or service is not authorized to perform this action", e);
        }
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headersMap = new HashMap<>();

        ArrayList<String> headerNames = Collections.list(request.getHeaderNames());
        for (String key : headerNames) {
            String value = request.getHeader(key);
            headersMap.put(key, value);
        }
        return headersMap;
    }
}