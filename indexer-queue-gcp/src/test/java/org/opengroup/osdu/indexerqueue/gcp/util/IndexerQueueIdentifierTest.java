// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.indexerqueue.gcp.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.core.gcp.util.HeadersInfo;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(SpringRunner.class)
public class IndexerQueueIdentifierTest {

    @Mock
    private ITenantInfoService tenantInfoService;
    @Mock
    private ITenantFactory tenantInfoServiceProvider;
    @Mock
    private DpsHeaders dpsHeaders;
    @Mock
    private HeadersInfo headersInfo;

    @InjectMocks
    private TenantInfo tenantInfo;
    @InjectMocks
    private IndexerQueueIdentifier sut;
    String expectedQueueId = "tenant1-os-indexer-queue-osdu";

    @Before
    public void setup() {
        this.tenantInfo.setName("tenant1");
        when(this.tenantInfoService.getTenantInfo()).thenReturn(tenantInfo);
        when(tenantInfoServiceProvider.getTenantInfo(Mockito.any())).thenReturn(tenantInfo);
        when(this.headersInfo.getHeaders()).thenReturn(dpsHeaders);
        when(dpsHeaders.getPartitionId()).thenReturn("dummy-partition-id");
    }

    @Test
    public void should_return_validTaskQueue() {
        assertEquals(expectedQueueId, this.sut.getQueueId());
    }
}
