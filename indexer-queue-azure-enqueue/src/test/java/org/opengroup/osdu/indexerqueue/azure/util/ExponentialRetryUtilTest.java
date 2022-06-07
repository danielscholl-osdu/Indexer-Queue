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
    private final int MAX_RETRY_DURATION = 43200; // 12 hours in seconds

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
        assertEquals(240, service.generateNextRetryTerm(4));    // 4 min
        assertEquals(900, service.generateNextRetryTerm(5));    // 15 min
        assertEquals(3240, service.generateNextRetryTerm(6));   // ~ 1 h
        assertEquals(11340, service.generateNextRetryTerm(7));  // ~ 3 h
        assertEquals(38880, service.generateNextRetryTerm(8));  // ~ 11 h
        assertEquals(43200, service.generateNextRetryTerm(9));  // 12 hours
        assertEquals(43200, service.generateNextRetryTerm(10)); // 12 hours
    }
}
