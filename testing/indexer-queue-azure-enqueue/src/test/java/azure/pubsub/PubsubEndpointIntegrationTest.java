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
package azure.pubsub;

import azure.utils.HeaderUtils;
import azure.utils.LegalTagUtils;
import azure.utils.TenantUtils;
import azure.utils.TestUtils;
import com.google.gson.*;
import com.sun.jersey.api.client.ClientResponse;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/***
 * Integration test to generate a new record using storage service and check whether it is searchable.
 * This flow checks that indexer-queue is successfully sending messages to indexer service.
 */
@Slf4j
public class PubsubEndpointIntegrationTest {
    // Generate unique record id using timestamp
    protected static final String RECORD_ID = TenantUtils.getTenantName() + ":inttest:" + System.currentTimeMillis();
    protected static final String KIND = TenantUtils.getTenantName() + ":wks:inttest:1.0."
            + System.currentTimeMillis();
    protected static String LEGAL_TAG = LegalTagUtils.createRandomName();

    /***
     * Create legal tag and create new unique record in storage service before running the test.
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        String token = TestUtils.getToken();
        LegalTagUtils.create(LEGAL_TAG, token);
    }

    /***
     * Delete the legal tag and record created after running the test.
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        String token = TestUtils.getToken();
        TestUtils.send("records/" + RECORD_ID, "DELETE", HeaderUtils.getHeaders(TenantUtils.getTenantName(), token), "", "");
        LegalTagUtils.delete(LEGAL_TAG, token);
    }

    /***
     * Call Search service query to verify that the new records generated have been indexed and are searchable.
     * @throws Exception
     */
    @Test
    public void should_beAbleToSearch_newlyCreatedRecords() throws Exception {
        // Create record in storage service

        String jsonInput = createJsonBody(RECORD_ID, "tian");
        ClientResponse storageServiceResponse = TestUtils.send("records", "PUT", HeaderUtils.getHeaders(TenantUtils.getTenantName(), TestUtils.getToken()), jsonInput, "");
        assertEquals(201, storageServiceResponse.getStatus());
        assertTrue(storageServiceResponse.getType().toString().contains("application/json"));

        // Sleep for one minute to wait for indexing to happen
        Thread.sleep(60000);
        String searchUrl=System.getProperty("SEARCH_URL", System.getenv("SEARCH_URL"));
        ClientResponse response = TestUtils.send(searchUrl, "query", "POST", HeaderUtils.getHeaders(TenantUtils.getTenantName(), TestUtils.getToken()), getSearchQueryRequestBody());
        String json = response.getEntity(String.class);
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        assertEquals(1,Integer.parseInt(jsonObject.get("totalCount").toString()));
    }

    public static String indentatedBody(String responseBody) {
        JsonParser jsonParser = new JsonParser();
        if(responseBody==null)
            return responseBody;
        JsonElement jsonElement = jsonParser.parse(responseBody);
        String indentedResponseEntity =new GsonBuilder().setPrettyPrinting().create().toJson(jsonElement);
        return indentedResponseEntity;
    }

    protected static String createJsonBody(String id, String name) {
        return "[" + singleEntityBody(id, name, KIND, LEGAL_TAG) + "]";
    }

    /***
     * Fetch request body for PUT record API in storage service. Sample request body:
     * [
     *        {
     * 		"id": "opendes:inttest:1626697759203",
     * 		"kind": "opendes:ds:inttest:1.0.1626697759203",
     * 		"acl": {
     * 			"viewers": [
     * 				"data.test1@opendes.contoso.com"
     * 			],
     * 			"owners": [
     * 				"data.test1@opendes.contoso.com"
     * 			]
     *        },
     * 		"legal": {
     * 			"legaltags": [
     * 				"opendes-storage-1626697759203"
     * 			],
     * 			"otherRelevantDataCountries": [
     * 				"BR"
     * 			]
     *        },
     * 		"data": {
     * 			"name": "tian"
     *        }
     *    }
     * ]
     * @param id record id for the record.
     * @param name part of data field for the record.
     * @param kind kind or schema id for the record.
     * @param legalTagName legal tag name for the record.
     * @return
     */
    public static String singleEntityBody(String id, String name, String kind, String legalTagName) {

        JsonObject data = new JsonObject();
        data.addProperty("name", name);

        JsonObject acl = new JsonObject();
        JsonArray acls = new JsonArray();
        acls.add(TestUtils.getAcl());
        acl.add("viewers", acls);
        acl.add("owners", acls);

        JsonObject legal = new JsonObject();
        JsonArray legals = new JsonArray();
        legals.add(legalTagName);
        legal.add("legaltags", legals);
        JsonArray ordc = new JsonArray();
        ordc.add("BR");
        legal.add("otherRelevantDataCountries", ordc);

        JsonObject record = new JsonObject();
        if (!Strings.isNullOrEmpty(id)) {
            record.addProperty("id", id);
        }

        record.addProperty("kind", kind);
        record.add("acl", acl);
        record.add("legal", legal);
        record.add("data", data);

        return record.toString();
    }

    /***
     * Create request body like this for Search service API call
     * {
     * "kind": KIND,
     * "query":"id:\"RECORD_ID\"",
     * }
     * @return request body json for search query
     */
    public static String getSearchQueryRequestBody() {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("kind", KIND);
        String query = String.format("id:\"%s\"",RECORD_ID);
        requestBody.addProperty("query",query);
        return requestBody.toString();
    }

}
