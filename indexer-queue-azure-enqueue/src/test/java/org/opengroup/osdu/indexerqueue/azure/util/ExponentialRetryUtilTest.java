package org.opengroup.osdu.indexerqueue.azure.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengroup.osdu.indexerqueue.azure.util.ExponentialRetryUtil.builder;

@ExtendWith(MockitoExtension.class)
public class ExponentialRetryUtilTest {

    private static final int ELONGATION_POINT = 3;
    private final int STANDARD_MULTIPLIER = 3;
    private final int MAX_RETRY_DURATION = 32400; // 9 hours in seconds

    private ExponentialRetryUtil service;

    @BeforeEach
    public void before() {
        service = builder()
                .elongationPoint(ELONGATION_POINT)
                .multiplier(STANDARD_MULTIPLIER)
                .maxRetryDuration(MAX_RETRY_DURATION)
                .build();
    }

    @Test
    public void shouldGenerateRetriesSuccessfully() {
        assertEquals(3, service.generateNextRetryTerm(1));
        assertEquals(12, service.generateNextRetryTerm(2));
        assertEquals(36, service.generateNextRetryTerm(3));
        assertEquals(204, service.generateNextRetryTerm(4));    // ~3 min
        assertEquals(765, service.generateNextRetryTerm(5));    // ~ 13 min
        assertEquals(2754, service.generateNextRetryTerm(6));   // ~ 46 mins
        assertEquals(9639, service.generateNextRetryTerm(7));  // ~ 2 h 30 min
        assertEquals(32400, service.generateNextRetryTerm(8));  //  9 h
        assertEquals(32400, service.generateNextRetryTerm(9));  // 9 h
    }

    @Test
    public void totalExecutionTime_for_8_retries_shouldBeApproxEqualTo_12_Hours() {
        int totalTime = 0;
        for (int i = 1; i < 9; i++){
            totalTime+= service.generateNextRetryTerm(i);
        }
        assertEquals(45813, totalTime); // ~ 12,5 hours
    }
}
