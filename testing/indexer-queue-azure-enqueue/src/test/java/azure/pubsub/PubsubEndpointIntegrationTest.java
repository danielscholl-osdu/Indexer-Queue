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

import azure.models.schema.SchemaIdentity;
import azure.models.schema.SchemaInfo;
import azure.models.schema.SchemaModel;
import azure.utils.HeaderUtils;
import azure.utils.LegalTagUtils;
import azure.utils.TenantUtils;
import azure.utils.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import com.sun.jersey.api.client.ClientResponse;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    protected static final String entityType = "kind"+System.currentTimeMillis();
    protected static final String SCHEMA_UPDATE_KIND = "osdu:kindupdate:"+entityType+":1.0.0";
    protected static String LEGAL_TAG = LegalTagUtils.createRandomName();
    protected static final String STORAGE_URL = System.getProperty("STORAGE_URL", System.getenv("STORAGE_URL"));
    protected static final String SEARCH_URL = System.getProperty("SEARCH_URL", System.getenv("SEARCH_URL"));
    protected static final String SCHEMA_URL = System.getProperty("SCHEMA_HOST", System.getenv("SCHEMA_HOST"))+"/";
    protected static final String INDEXER_URL = System.getProperty("INDEXER_URL", System.getenv("INDEXER_URL"));

    protected static ObjectMapper mapper = new ObjectMapper();
    protected static Gson gson = new Gson();
    
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
        TestUtils.send(STORAGE_URL, "records/" + RECORD_ID, "DELETE", HeaderUtils.getHeaders(TenantUtils.getTenantName(), token), "", "");
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
        ClientResponse storageServiceResponse = TestUtils.send(STORAGE_URL, "records", "PUT", HeaderUtils.getHeaders(TenantUtils.getTenantName(), TestUtils.getToken()), jsonInput, "");
        assertEquals(201, storageServiceResponse.getStatus());
        assertTrue(storageServiceResponse.getType().toString().contains("application/json"));

        // Sleep for one minute to wait for indexing to happen
        Thread.sleep(60000);
        ClientResponse response = TestUtils.send(SEARCH_URL, "query", "POST", HeaderUtils.getHeaders(TenantUtils.getTenantName(), TestUtils.getToken()), getSearchQueryRequestBody(), "");
        String json = response.getEntity(String.class);
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        assertEquals(1,Integer.parseInt(jsonObject.get("totalCount").toString()));
    }

    @Test
    public void should_beAbleToSearchRecord_whenSchemaChanges() throws Exception {
        //create a new kind via Schema service
        String schemaPayload = getInitialSchemaPayload();
        TestUtils.send(SCHEMA_URL, "schema", "POST", HeaderUtils.getHeaders(TenantUtils.getTenantName(), TestUtils.getToken()), schemaPayload, "");

        //create a new record via Storage service using created kind
        String jsonInput = createJsonBodyWithKind(SCHEMA_UPDATE_KIND, "john", "");
        ClientResponse storageServiceResponse = TestUtils.send(STORAGE_URL, "records", "PUT", HeaderUtils.getHeaders(TenantUtils.getTenantName(), TestUtils.getToken()), jsonInput, "");
        assertEquals(201, storageServiceResponse.getStatus());

        //record should be searchable via Search service
        Thread.sleep(60000);
        ClientResponse response = TestUtils.send(SEARCH_URL, "query", "POST", HeaderUtils.getHeaders(TenantUtils.getTenantName(), TestUtils.getToken()), getSearchQueryRequestBodyQueryWithFirstName(SCHEMA_UPDATE_KIND, "john"), "");
        String json = response.getEntity(String.class);
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        assertEquals(1,Integer.parseInt(jsonObject.get("totalCount").toString()));

        //modify kind via schema service
        String modifySchemaPayalod = getUpdatedSchemaPayload();
        TestUtils.send(SCHEMA_URL, "schema", "PUT", HeaderUtils.getHeaders(TenantUtils.getTenantName(), TestUtils.getToken()), modifySchemaPayalod, "");

        //create a new record via Storage service using modified kind
        jsonInput = createJsonBodyWithKind(SCHEMA_UPDATE_KIND, "john", "doe");
        storageServiceResponse = TestUtils.send(STORAGE_URL, "records", "PUT", HeaderUtils.getHeaders(TenantUtils.getTenantName(), TestUtils.getToken()), jsonInput, "");
        assertEquals(201, storageServiceResponse.getStatus());

        //new record should be searchable with modified query
        Thread.sleep(60000);
        response = TestUtils.send(SEARCH_URL, "query", "POST", HeaderUtils.getHeaders(TenantUtils.getTenantName(), TestUtils.getToken()), getSearchQueryRequestBodyQueryWithLastName(SCHEMA_UPDATE_KIND, "doe"), "");
        json = response.getEntity(String.class);
        jsonObject = new JsonParser().parse(json).getAsJsonObject();
        assertEquals(1,Integer.parseInt(jsonObject.get("totalCount").toString()));

        //clean up index
        String kindParam = "?kind="+SCHEMA_UPDATE_KIND;
        TestUtils.send(INDEXER_URL, "index", "DELETE", HeaderUtils.getHeaders(TenantUtils.getTenantName(), TestUtils.getToken()), "", kindParam);
    }


    protected static String createJsonBody(String id, String name) {
        return "[" + singleEntityBody(id, name, "", KIND, LEGAL_TAG) + "]";
    }

    protected static String createJsonBodyWithKind(String kind, String firstname, String lastname) {
        return "[" + singleEntityBody("", firstname, lastname, kind, LEGAL_TAG) + "]";
    }

    protected static String getInitialSchemaPayload() throws Exception {
        SchemaModel schemaModel = new SchemaModel();
        schemaModel.setSchemaInfo(getSchemaInfo());
        schemaModel.setSchema(mapper.readValue(getInitialSchema(), Object.class));
        return gson.toJson(schemaModel);
    }

    protected static String getUpdatedSchemaPayload() throws Exception {
        SchemaModel schemaModel = new SchemaModel();
        schemaModel.setSchemaInfo(getSchemaInfo());
        schemaModel.setSchema(mapper.readValue(getModifiedSchema(), Object.class));
        return gson.toJson(schemaModel);
    }

    private static SchemaInfo getSchemaInfo() {
        SchemaInfo schemaInfo = new SchemaInfo();
        SchemaIdentity schemaIdentity = new SchemaIdentity();
        schemaIdentity.setAuthority("osdu");
        schemaIdentity.setSource("kindupdate");
        schemaIdentity.setEntityType(entityType);
        schemaIdentity.setSchemaVersionMajor("1");
        schemaIdentity.setSchemaVersionMinor("0");
        schemaIdentity.setSchemaVersionPatch("0");
        schemaInfo.setSchemaIdentity(schemaIdentity);
        schemaInfo.setStatus(SchemaInfo.SchemaStatus.DEVELOPMENT);
        return schemaInfo;
    }

    private static String getInitialSchema() {
        return "{\n" +
            "        \"properties\": {\n" +
            "            \"data\": {\n" +
            "                \"allOf\": [\n" +
            "                    {\n" +
            "                        \"type\": \"object\",\n" +
            "                        \"properties\": {\n" +
            "                            \"firstname\": {\n" +
            "                                \"type\": \"string\"\n" +
            "                            }\n" +
            "                        }\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    }";
    }

    private static String getModifiedSchema() {
        return "{\n" +
            "        \"properties\": {\n" +
            "            \"data\": {\n" +
            "                \"allOf\": [\n" +
            "                    {\n" +
            "                        \"type\": \"object\",\n" +
            "                        \"properties\": {\n" +
            "                            \"firstname\": {\n" +
            "                                \"type\": \"string\"\n" +
            "                            },\n" +
            "                            \"lastname\": {\n" +
            "                                \"type\": \"string\"\n" +
            "                            }\n" +
            "                        }\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    }";
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
     * 			"firstname": "tian"
     *        }
     *    }
     * ]
     * @param id record id for the record.
     * @param firstname part of data field for the record.
     * @param lastname part of data field for the record.
     * @param kind
     * @param legalTagName legal tag name for the record.
     * @return
     */
    public static String singleEntityBody(String id, String firstname, String lastname, String kind, String legalTagName) {

        JsonObject data = new JsonObject();
        data.addProperty("firstname", firstname);
        if (!Strings.isNullOrEmpty(lastname))
            data.addProperty("lastname", lastname);

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

    public static String getSearchQueryRequestBodyQueryWithFirstName(String kind, String name) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("kind", kind);
        String query = String.format("data.firstname:\"%s\"", name);
        requestBody.addProperty("query",query);
        return requestBody.toString();
    }

    public static String getSearchQueryRequestBodyQueryWithLastName(String kind, String name) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("kind", kind);
        String query = String.format("data.lastname:\"%s\"", name);
        requestBody.addProperty("query",query);
        return requestBody.toString();
    }

}
