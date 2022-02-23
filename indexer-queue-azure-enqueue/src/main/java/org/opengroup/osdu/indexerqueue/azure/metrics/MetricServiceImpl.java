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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetricServiceImpl implements IMetricService {

    @Autowired
    private TelemetryClient telemetryClient;

    private static final String LATENCY_METRIC_NAME = "[Indexer service] Record indexing latency";

    @Override
    public String getLatencyMetricName() {
        return LATENCY_METRIC_NAME;
    }

    @Override
    public void sendIndexLatencyMetric(double value) {
        this.sendMetric(LATENCY_METRIC_NAME, value);
    }

    private void sendMetric(String name, double value) {
        MetricTelemetry metric = new MetricTelemetry();
        metric.setName(name);
        metric.setValue(value);
        telemetryClient.trackMetric(metric);
        telemetryClient.flush();
    }
}

