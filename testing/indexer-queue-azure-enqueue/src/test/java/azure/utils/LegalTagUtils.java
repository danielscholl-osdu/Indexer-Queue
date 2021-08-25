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

package azure.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.jersey.api.client.ClientResponse;

public class LegalTagUtils {
    public static String createRandomName() {
        return TenantUtils.getTenantName() + "-storage-" + System.currentTimeMillis();
    }

    public static ClientResponse create(String legalTagName, String token) throws Exception {
        return create("US", legalTagName, "2099-01-25", "Public Domain Data", token);
    }

    protected static ClientResponse create(String countryOfOrigin, String name, String expDate, String dataType, String token)
            throws Exception {
        String body = getBody(countryOfOrigin, name, expDate, dataType);
        ClientResponse response = TestUtils.send(getLegalUrl(), "legaltags", "POST", HeaderUtils.getHeaders(TenantUtils.getTenantName(), token), body,
                "");

        Thread.sleep(100);
        return response;
    }

    public static ClientResponse delete(String legalTagName, String token) throws Exception {
        return TestUtils.send(getLegalUrl(), "legaltags/" + legalTagName, "DELETE", HeaderUtils.getHeaders(TenantUtils.getTenantName(), token), "", "");
    }

    protected static String getLegalUrl() {
        String legalUrl = System.getProperty("LEGAL_URL", System.getenv("LEGAL_URL"));
        if (legalUrl == null || legalUrl.contains("-null")) {
            throw new IllegalArgumentException("LEGAL_URL value is not set correctly");
        }
        return legalUrl;
    }

    protected static String getBody(String countryOfOrigin, String name, String expDate, String dataType) {

        JsonArray coo = new JsonArray();
        coo.add(countryOfOrigin);

        JsonObject properties = new JsonObject();
        properties.add("countryOfOrigin", coo);
        properties.addProperty("contractId", "A1234");
        properties.addProperty("expirationDate", expDate);
        properties.addProperty("dataType", dataType);
        properties.addProperty("originator", "MyCompany");
        properties.addProperty("securityClassification", "Public");
        properties.addProperty("exportClassification", "EAR99");
        properties.addProperty("personalData", "No Personal Data");

        JsonObject tag = new JsonObject();
        tag.addProperty("name", name);
        tag.addProperty("description", "test for " + name);
        tag.add("properties", properties);

        return tag.toString();
    }
}
