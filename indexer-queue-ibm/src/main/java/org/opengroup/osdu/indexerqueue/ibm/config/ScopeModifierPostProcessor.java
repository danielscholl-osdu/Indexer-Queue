/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.indexerqueue.ibm.config;

import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.indexerqueue.ibm.thread.ThreadScope;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class ScopeModifierPostProcessor implements BeanFactoryPostProcessor {

    public static final String SCOPE_THREAD = "scope_thread";

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) throws BeansException {
        factory.registerScope(SCOPE_THREAD, new ThreadScope());

        for (String beanName : factory.getBeanDefinitionNames()) {
            BeanDefinition beanDef = factory.getBeanDefinition(beanName);
            if (Objects.equals(beanDef.getScope(), "request")) {
                beanDef.setScope(SCOPE_THREAD);
                log.debug("Scope has been overridden for bean: {}", beanDef.getBeanClassName());
            }
        }
    }
}
