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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ServiceAccountJwtClientImplTest {

    private String partitionId = "opendes";
    private static String authorizationToken = "Bearer authorizationToken";
    private static String token = "token";

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

    @Test
    public void should_prefixBearer_whenTokenDoesNotStartWithBearer() {
        when(tokenService.getAuthorizationToken()).thenReturn(token);

        String authToken = sut.getIdToken(partitionId);

        verify(tokenService, times(1)).getAuthorizationToken();
        assertEquals("Bearer " + token, authToken);
    }

}
