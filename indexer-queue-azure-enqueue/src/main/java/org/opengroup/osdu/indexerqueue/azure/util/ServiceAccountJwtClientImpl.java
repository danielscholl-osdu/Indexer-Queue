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

import com.google.common.base.Strings;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceAccountJwtClientImpl implements IServiceAccountJwtClient {
    private final String BEARER = "Bearer";

    @Autowired
    private AzureServicePrincipleTokenService tokenService;

    @Override
    public String getIdToken(String partitionId) {
        String token = this.tokenService.getAuthorizationToken();
        if (!Strings.isNullOrEmpty(token) && !token.startsWith(BEARER)) {
            token = BEARER + " " + token;
        }
        return token;
    }
}
