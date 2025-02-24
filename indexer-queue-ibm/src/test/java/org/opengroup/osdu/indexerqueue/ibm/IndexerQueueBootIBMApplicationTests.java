package org.opengroup.osdu.indexerqueue.ibm;

//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import org.apache.commons.lang3.StringUtils;
//import org.junit.Test;
import org.junit.runner.RunWith;
//import org.opengroup.osdu.core.common.Constants;
//import org.opengroup.osdu.core.common.http.HttpClient;
//import org.opengroup.osdu.core.common.http.HttpRequest;
//import org.opengroup.osdu.core.common.http.HttpResponse;
//import org.opengroup.osdu.core.common.model.http.AppError;
//import org.opengroup.osdu.core.common.model.http.DpsHeaders;
//import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
//import org.opengroup.osdu.core.ibm.util.Config;
import org.springframework.test.context.junit4.SpringRunner;

//import com.google.gson.Gson;

//@RunWith(SpringRunner.class)
public class IndexerQueueBootIBMApplicationTests {

//    private static final String CORRELATION_ID = "1234";
//    private static final String OPENDES = "opendes";
//	private final Gson gson = new Gson();
//	
//	String data = "[{\"id\":\"opendes:doc:1234\",\"kind\":\"opendes:test:test:1.0.0\",\"op\":\"create\"}]";
//		
//	String INDEXER_API_KEY = Config.getEnvironmentVariable("INDEXER_API_KEY");
//	String INDEXER_HOST_URL = Config.getEnvironmentVariable("INDEXER_HOST_URL");
//		
//	
//    @Test
//    public void should_returnErrorCode401_when_apiKeyMismatched() {
//    	// In case of api key mismatch at indexer end, indexer throws 
//    	// AppException i.e Token in header mismatched. same needs to check in test case 
//    	
//        String incorrectApiKey = "incorrectApiKey"+System.currentTimeMillis();
//    	RecordChangedMessages recordChangeMessage = new RecordChangedMessages();
//    	
//    	Map<String, String> attributes = setCorrelationAndDataPartitionIds();
//    	recordChangeMessage.setAttributes(attributes);
//    	recordChangeMessage.setData(data);
//    	
//    	String url = StringUtils.join(INDEXER_HOST_URL, Constants.WORKER_RELATIVE_URL);
//		HttpClient httpClient = new HttpClient();
//		DpsHeaders dpsHeaders = new DpsHeaders();
//		dpsHeaders.put("x-api-key", incorrectApiKey);
//		dpsHeaders.put("correlation-id", CORRELATION_ID);
//		dpsHeaders.put("data-partition-id", OPENDES);
//		
//		HttpRequest rq = HttpRequest.post(recordChangeMessage).url(url).headers(dpsHeaders.getHeaders()).build();
//		HttpResponse result = httpClient.send(rq);
//		AppError error = gson.fromJson(result.getBody(), AppError.class);
//		
//		assertEquals(error.getMessage(), "Token in header mismatched.");
//    }
//
//    @Test
//    public void should_returnErrorCode500_when_apiKeyMatched() {
//    	// If api key matched at indexer then normal flow will continue but expected 
//    	// record do not found at storage because recordChangeMessage has dummy data hence 
//    	// 500 error is thrown
//    	
//    	RecordChangedMessages recordChangeMessage = new RecordChangedMessages();
//    	
//    	Map<String, String> attributes = setCorrelationAndDataPartitionIds();
//    	recordChangeMessage.setAttributes(attributes);
//    	recordChangeMessage.setData(data);
//    	
//    	String url = StringUtils.join(INDEXER_HOST_URL, Constants.WORKER_RELATIVE_URL);
//		HttpClient httpClient = new HttpClient();
//		DpsHeaders dpsHeaders = new DpsHeaders();
//		dpsHeaders.put("x-api-key", INDEXER_API_KEY);
//		dpsHeaders.put("correlation-id", CORRELATION_ID);
//		dpsHeaders.put("data-partition-id", OPENDES);
//		
//		HttpRequest rq = HttpRequest.post(recordChangeMessage).url(url).headers(dpsHeaders.getHeaders()).build();
//		HttpResponse result = httpClient.send(rq);
//		
//		assertTrue(result.getResponseCode() == 500);
//		
//    }
//    
//    private Map<String, String> setCorrelationAndDataPartitionIds() {
//		Map<String, String> attributes = new HashMap<>();
//    	attributes.put("correlation-id", CORRELATION_ID);
//    	attributes.put("data-partition-id", OPENDES);
//		return attributes;
//	}
}
