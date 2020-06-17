// Copyright 2017-2019, Schlumberger//// Licensed under the Apache License, Version 2.0 (the "License");// you may not use this file except in compliance with the License.// You may obtain a copy of the License at////      http://www.apache.org/licenses/LICENSE-2.0//// Unless required by applicable law or agreed to in writing, software// distributed under the License is distributed on an "AS IS" BASIS,// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.// See the License for the specific language governing permissions and// limitations under the License.package org.opengroup.osdu.indexerqueue.gcp.middleware;import org.opengroup.osdu.core.common.Constants;import org.opengroup.osdu.core.common.model.http.AppException;import org.opengroup.osdu.core.common.model.http.DpsHeaders;import org.opengroup.osdu.core.gcp.model.AppEngineHeaders;import org.opengroup.osdu.core.gcp.util.HeadersInfo;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;import javax.servlet.*;import javax.servlet.http.HttpServletRequest;import java.io.IOException;@Componentpublic class RedirectHttpRequestsHandler implements Filter{    @Autowired    private HeadersInfo headersInfo;    private FilterConfig filterConfig;    @Value("${ACCEPT_HTTP:false}")    private boolean acceptHttp;    @Override    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {        HttpServletRequest httpRequest = (HttpServletRequest) request;        // return 302 redirect if http connection is attempted        if (!httpRequest.isSecure() && !isAcceptHttp()) {            throw new AppException(302, "Redirect", "HTTP is not supported. Use HTTPS.");        }        if (isTaskQueueRequest(this.headersInfo.getHeaders())) return;        filterChain.doFilter(request,response);    }    public void init(final FilterConfig filterConfig)    {        this.filterConfig = filterConfig;    }    public void destroy()    {        this.filterConfig = null;    }    public boolean isAcceptHttp() {        return acceptHttp;    }    private boolean isTaskQueueRequest(DpsHeaders dpsHeaders) {        if (!dpsHeaders.getHeaders().containsKey(AppEngineHeaders.TASK_QUEUE_NAME)) return false;        String queueId = dpsHeaders.getHeaders().get(AppEngineHeaders.TASK_QUEUE_NAME);        return queueId.endsWith(Constants.INDEXER_QUEUE_IDENTIFIER);    }}