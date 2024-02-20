/* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexerqueue.aws.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentVariablesTest {

    @Rule
    public EnvironmentVariablesRule environmentVariablesRule = new EnvironmentVariablesRule();

    private void verifyEnvironmentVariables(EnvironmentVariables instance) {
        Exception e = null;
        try {
            instance.getDeadLetterQueueUrl();
            assertNotNull(instance.getRegion());
            instance.getQueueUrl();
            instance.getTargetURL();
            assertNotEquals(0, instance.getMaxAllowedMessages());
            assertNotEquals(0, instance.getMaxWaitTime());
            assertNotEquals(0, instance.getMaxIndexThreads());
            assertNotEquals(0, instance.getMaxBatchRequestCount());
        } catch (Exception error) {
            e = error;
        }
        assertNull(e);
    }

    @Test
    public void should_correctlyLoadParameters_when_propertiesExist() throws Exception {
        environmentVariablesRule.set("PARAMETER_MOUNT_PATH", ".");

        EnvironmentVariables testInstance = new EnvironmentVariables();
        verifyEnvironmentVariables(testInstance);
    }
}
