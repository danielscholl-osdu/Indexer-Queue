// Copyright © Schlumberger
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

package org.opengroup.osdu.indexerqueue.azure.metrics;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MetricServiceTest {

    @Mock
    private TelemetryClient telemetryClient;

    @InjectMocks
    private MetricServiceImpl sut;

    private static final String metricName = "[Indexer service] Record indexing latency";

    @Test
    public void shouldSendHitsMetricSuccessfully() {
        ArgumentCaptor<MetricTelemetry> metricCaptor = ArgumentCaptor.forClass(MetricTelemetry.class);

        this.sut.sendIndexLatencyMetric(1l, "records-changed", "opendes", "correlationId-1", true);

        verify(telemetryClient, times(1)).trackMetric(metricCaptor.capture());
        assertEquals("[Indexer service] Record indexing latency", metricCaptor.getValue().getName());
        assertEquals(1, metricCaptor.getValue().getValue(), 1);
        assertEquals("true", metricCaptor.getValue().getProperties().get("success"));
        assertEquals("correlationId-1", metricCaptor.getValue().getProperties().get("correlation-id"));
        assertEquals("opendes", metricCaptor.getValue().getProperties().get("data-partition-id"));
        assertEquals("records-changed", metricCaptor.getValue().getProperties().get("topic"));
        verify(telemetryClient, times(1)).flush();
    }

    @Test
    public void shouldReturnCorrectLatencyMetricName() {
        String latencyMetricName = sut.getLatencyMetricName();
        assertEquals(metricName, latencyMetricName);
    }
}
