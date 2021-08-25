package org.opengroup.osdu.indexerqueue.azure.config;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Primary
@Scope(value = "ThreadScope", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ThreadDpsHeaders extends DpsHeaders {
    public void setThreadContext(String dataPartitionId, String correlationId, String accountId) {
        Map<String, String> headers = new HashMap<>();
        headers.put(DpsHeaders.DATA_PARTITION_ID, dataPartitionId);
        headers.put(DpsHeaders.CORRELATION_ID, correlationId);
        headers.put(DpsHeaders.ACCOUNT_ID, accountId);
        this.addFromMap(headers);
    }
}
