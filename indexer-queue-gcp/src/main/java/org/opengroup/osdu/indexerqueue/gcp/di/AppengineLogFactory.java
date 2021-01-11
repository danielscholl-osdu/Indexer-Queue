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

package org.opengroup.osdu.indexerqueue.gcp.di;

import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.gcp.logging.logger.appengine.AppEngineLoggingProvider;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name="disable.appengine.log.factory", havingValue = "false", matchIfMissing = true )
@Component
@Primary
@Lazy
public class AppengineLogFactory implements FactoryBean<ILogger> {

    private AppEngineLoggingProvider appEngineLoggingProvider = new AppEngineLoggingProvider();

    @Override
    public ILogger getObject() throws Exception {
        return appEngineLoggingProvider.getLogger();
    }

    @Override
    public Class<?> getObjectType() {
        return ILogger.class;
    }
}