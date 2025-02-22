package org.opengroup.osdu.indexerqueue.azure.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class MdcContextMapTest {

    @InjectMocks
    MdcContextMap mdcContextMap;

    private static final String correlationId = "test-correlation-id";
    private static final String dataPartitionId = "test-data-partition-id";

    @Test
    public void shouldReturnContextMapWithProvidedIds() {
        Map<String, String> contextMap = mdcContextMap.getContextMap(correlationId, dataPartitionId);

        assertEquals(2, contextMap.size());
        assertEquals(correlationId, contextMap.get("correlation-id"));
        assertEquals(dataPartitionId, contextMap.get("data-partition-id"));
    }

}
