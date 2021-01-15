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

package org.opengroup.osdu.indexerqueue.gcp.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties()
@Getter
@Setter
@ToString
public class IndexerQueueConfigurationPropertiesGcp {

	//Default Cache Settings
	private Integer schemaCacheExpiration = 60;
	private Integer indexCacheExpiration = 60;
	private Integer elasticCacheExpiration = 1440;
	private Integer cursorCacheExpiration = 60;

	//Kinds Cache expiration 2*24*60
	private Integer kindsCacheExpiration = 2880;

	//Attributes Cache expiration 2*24*60
	private Integer attributesCacheExpiration = 2880;

	private Integer kindsRedisDatabase = 1;

	private String environment;
	private String indexerHost;

	private String googleCloudProject;
	private String googleCloudProjectRegion;
	private String googleAudiences;

	public String getDeploymentLocation() {
		return googleCloudProjectRegion;
	}
}
