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

package org.opengroup.osdu.indexerqueue.gcp.cloudrun.di;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.entitlements.EntitlementsAPIConfig;
import org.opengroup.osdu.core.common.entitlements.EntitlementsFactory;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.config.EntitlementProperties;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class EntitlementsClientFactory extends AbstractFactoryBean<IEntitlementsFactory> {

	private final EntitlementProperties entitlementProperties;
	private final HttpResponseBodyMapper mapper;

	@Override
	protected IEntitlementsFactory createInstance() {
		return new EntitlementsFactory(EntitlementsAPIConfig.builder()
			.rootUrl(entitlementProperties.getUrl())
			.apiKey(entitlementProperties.getAppKey())
			.build(), mapper);
	}

	@Override
	public Class<?> getObjectType() {
		return IEntitlementsFactory.class;
	}
}
