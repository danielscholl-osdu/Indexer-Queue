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

package org.opengroup.osdu.indexerqueue.azure.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAccountJwtClientImplTest {

    private String partitionId = "opendes";
    private static String authorizationToken = "Bearer authorizationToken";

    @Mock
    private AzureServicePrincipleTokenService tokenService;

    @InjectMocks
    private ServiceAccountJwtClientImpl sut;

    @Test
    public void should_invoke_methodsWithRightArguments_andReturnAuthToken_when_getIdToken_isCalled() {
        when(tokenService.getAuthorizationToken()).thenReturn(authorizationToken);

        String authToken = sut.getIdToken(partitionId);

        verify(tokenService, times(1)).getAuthorizationToken();
        assertEquals(authorizationToken, authToken);
    }
}
