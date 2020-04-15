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

import lombok.extern.java.Log;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Log
@Component
@RequestScope
public class IndexerQueueIdentifier {

    @Autowired
    private ITenantFactory tenantInfoServiceProvider;

    @Autowired
    private TenantInfo tenant;

    @Autowired
    private DpsHeaders dpsHeaders;

    public String getQueueId() {
        if (this.tenantInfoServiceProvider == null) {
            log.info("ITENANT FACTORY OBJECT is NULL");
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "ITenant factory object is Null", "ITenant factory object is Null");
        }
        tenant = this.tenantInfoServiceProvider.getTenantInfo(dpsHeaders.getPartitionId());
        if (tenant == null) {
            return("common");
        }

        return String.format("%s-os-%s", tenant.getName(), Constants.INDEXER_QUEUE_IDENTIFIER);
    }
}