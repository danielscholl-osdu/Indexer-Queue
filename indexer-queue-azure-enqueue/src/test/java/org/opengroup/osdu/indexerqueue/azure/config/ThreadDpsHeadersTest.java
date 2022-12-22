package org.opengroup.osdu.indexerqueue.azure.config;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ThreadDpsHeadersTest {

    final static private String dataPartitionId = "data-partition-id";
    final static private String correlationId = "correlation-id";
    final static private String accountId = "account-id";

    @InjectMocks
    ThreadDpsHeaders sut = new ThreadDpsHeaders();

    @Test
    void should_setThreadContext() {
        sut.setThreadContext(dataPartitionId, correlationId);

        assertEquals(dataPartitionId, sut.getPartitionId());
        assertEquals(correlationId, sut.getCorrelationId());
    }
}
