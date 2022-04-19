package org.opengroup.osdu.indexerqueue.azure.config;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

import static org.junit.jupiter.api.Assertions.*;

class ThreadDpsHeadersTest {

    final static private String dataPartitionId = "data-partition-id";
    final static private String correlationId = "correlation-id";
    final static private String accountId = "account-id";

    @InjectMocks
    ThreadDpsHeaders sut = new ThreadDpsHeaders();

    @Test
    void should_setThreadContext() {
        sut.setThreadContext(dataPartitionId, correlationId, accountId);
        assertEquals(dataPartitionId, DpsHeaders.DATA_PARTITION_ID);
        assertEquals(correlationId, DpsHeaders.CORRELATION_ID);
        assertEquals(accountId, DpsHeaders.ACCOUNT_ID);
    }
}