package org.opengroup.osdu.indexerqueue.azure.util;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;

import java.util.HashMap;
import java.util.Map;

public class MDCContextMap {

    public Map<String, String> getContextMap(String correlationId, String dataPartitionId) {
        final Map<String, String> contextMap = new HashMap<>();
        contextMap.put(DpsHeaders.CORRELATION_ID, correlationId);
        contextMap.put(DpsHeaders.DATA_PARTITION_ID, dataPartitionId);
        return contextMap;
    }
}
