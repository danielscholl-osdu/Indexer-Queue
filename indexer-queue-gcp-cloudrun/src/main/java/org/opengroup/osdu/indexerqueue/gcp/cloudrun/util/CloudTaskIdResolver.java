/*
 * Copyright 2020 Google LLC
 * Copyright 2020 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexerqueue.gcp.cloudrun.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.config.IndexerQueueConfigProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudTaskIdResolver {

  private static final String DEFAULT_QUEUE_ID = "common";

  private final ITenantFactory tenantInfoServiceProvider;
  private final IndexerQueueConfigProperties configuration;

  public String getQueueId(String partitionId) {
    TenantInfo tenant = getTenantInfo(partitionId);
    return tenant != null
        ? String.format("%s-%s", tenant.getName(), configuration.getIndexerQueueIdentifier())
        : DEFAULT_QUEUE_ID;
  }

  private TenantInfo getTenantInfo(String partitionId) {
    if (this.tenantInfoServiceProvider == null) {
      log.info("ITENANT FACTORY OBJECT is NULL");
      throw new AppException(HttpStatus.SC_BAD_REQUEST, "ITenant factory object is Null",
          "ITenant factory object is Null");
    }

    return this.tenantInfoServiceProvider.getTenantInfo(partitionId);
  }
}