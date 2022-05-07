// Copyright 2020 IBM Corp. All Rights Reserved.
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

package org.opengroup.osdu.indexerqueue.ibm.subscribe;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.http.HttpClient;
import org.opengroup.osdu.core.common.http.HttpRequest;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.ibm.messagebus.IMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

@Component
public class Subscriber {

	@Inject
	IMessageFactory mq;

	@Value("${INDEXER_URL}")
	private String INDEXER_URL;

	@Value("${RETRY_COUNT}")
	private int RETRY_COUNT;
	
	@Value("${INDEXER_API_KEY}")
	private String INDEXER_API_KEY;
	
	/*
	 * false : Messages will be indexed from Queue 
	 * true : Messages will be indexed  from TOPIC
	 */
	@Value("${ibm.topic.enable:false}")
	private String topicFlag;

	private final Gson gson = new Gson();
	private static final Logger logger = LoggerFactory.getLogger(Subscriber.class);
	private final DpsHeaders dpsHeaders = new DpsHeaders();
	private final static String RETRY_STRING = "retry";
	private final static String ERROR_CODE = "errorCode";
	private final static String ERROR_MESSAGE = "errorMessage";
	private Map<String, String> attributes;

	final String INDEXER_API_KEY_HEADER = "x-api-key";
	 
	@JmsListener(destination = "${ibm.env.prefix}" + "-" + IMessageFactory.DEFAULT_QUEUE_NAME)
	public void recievedMessage(String msg) throws Exception {

		logger.info("Recieved Message: " + msg);
		
		if(topicFlag.equalsIgnoreCase("true")) {
        	logger.info("Indexing api will not be called or disable flag 'ibm.topic.enable'. Indexing will be happen on messages from Topic");
        	return;
        }

		RecordChangedMessages recordMessage;

		if (msg.contains(RETRY_STRING)) {
			// handle failed records ibm-prefix-record queue
			recordMessage = this.gson.fromJson(msg, RecordChangedMessages.class);
			logger.info(String.format("message headers: %s", this.dpsHeaders.toString()));
			logger.info(String.format("message body: %s", this.gson.toJson(recordMessage)));

			if (!recordMessage.getAttributes().get(RETRY_STRING).isEmpty()
					&& Integer.parseInt(recordMessage.getAttributes().get(RETRY_STRING)) >= RETRY_COUNT) {
				recordMessage.getAttributes().put(RETRY_STRING, "0");
				msg = gson.toJson(recordMessage);
				logger.info("sending to dlq. Resetting retry count to 0");
				mq.sendMessageDLQ(msg);
				return;
			}
		}

		else {
			// handles fresh messages from os-storage service - ibm-prefix-record queue
			recordMessage = this.getTaskQueueMessage(msg);
			logger.info(String.format("recordMessage: %s", recordMessage.toString()));

			this.dpsHeaders.getHeaders().put(DpsHeaders.ACCOUNT_ID, recordMessage.getDataPartitionId());
			this.dpsHeaders.getHeaders().put(DpsHeaders.DATA_PARTITION_ID, recordMessage.getDataPartitionId());
			if (recordMessage.hasCorrelationId()) {
				this.dpsHeaders.getHeaders().put(DpsHeaders.CORRELATION_ID, recordMessage.getCorrelationId());
			}
			logger.info(String.format("message headers: %s", this.dpsHeaders.toString()));
			logger.info(String.format("message body: %s", this.gson.toJson(recordMessage)));
		}

		String url = StringUtils.join(INDEXER_URL, Constants.WORKER_RELATIVE_URL);
		HttpClient httpClient = new HttpClient();
		this.dpsHeaders.put(INDEXER_API_KEY_HEADER, INDEXER_API_KEY);
		HttpRequest rq = HttpRequest.post(recordMessage).url(url).headers(this.dpsHeaders.getHeaders()).build();
		HttpResponse result = httpClient.send(rq);
		if (result.hasException()) {
			// extract exception info from result body and add attribute in recodchangedMessage
			logger.error(result.getException().getLocalizedMessage(), result.getException());
			int retryCount = getRetryCount(recordMessage);
			String responseCode = String.valueOf(result.getResponseCode());
			attributes = recordMessage.getAttributes();
			attributes.put(ERROR_CODE, responseCode);
			attributes.put(ERROR_MESSAGE, result.getException().getMessage());
			attributes.put(RETRY_STRING, String.valueOf(retryCount));
			recordMessage.setAttributes(attributes);
			msg = gson.toJson(recordMessage);
			mq.sendMessage(msg);
			return;
		} else if (result.getResponseCode() != 200) {
			// if AppException thrown from os-indexer module then add errorcode and errormessage into attributes
			int retryCount = getRetryCount(recordMessage);
			String responseCode = String.valueOf(result.getResponseCode());
			logger.error(String.format("Error ResponseCode: %s", responseCode));
			String errMsg = "";
			try {
				AppError error = gson.fromJson(result.getBody(), AppError.class);
				logger.error(String.format("Error Response: %s", error.toString()));
				errMsg = error.getMessage();
			} catch (JsonSyntaxException e) {
				logger.error(String.format("Failed to parse the error response body: %s encountered %s",
						result.getBody(), e.getMessage()));
			}
			attributes = recordMessage.getAttributes();
			attributes.put(ERROR_MESSAGE, errMsg);
			attributes.put(ERROR_CODE, responseCode);
			attributes.put(RETRY_STRING, String.valueOf(retryCount));
			recordMessage.setAttributes(attributes);
			msg = gson.toJson(recordMessage);
			mq.sendMessage(msg);
		}

	}

	/**
	 * @param recordMessage
	 * @return
	 */
	private int getRetryCount(RecordChangedMessages recordMessage) {
		int retryCount = 0;
		if (recordMessage.getAttributes().containsKey(RETRY_STRING)) {
			retryCount = Integer.parseInt(recordMessage.getAttributes().get(RETRY_STRING));
			retryCount++;
		} else {
			retryCount = 1;
		}
		return retryCount;
	}

	private RecordChangedMessages getTaskQueueMessage(String msg) {
		try {
			JsonParser jsonParser = new JsonParser();
			JsonElement jsonMessage = jsonParser.parse(msg);

			RecordChangedMessages recordChangedMessages = this.gson.fromJson(jsonMessage.toString(),
					RecordChangedMessages.class);
			String payload = recordChangedMessages.getData();
			if (Strings.isNullOrEmpty(payload)) {
				logger.error("message data not found");
				throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid record change message",
						"message data not found", "'message.data' not found in PubSub message");
			}

			// TODO alanbraz our messages are not encoded
			// String decodedPayload = new String(Base64.getDecoder().decode(payload));
			// recordChangedMessages.setData(decodedPayload);

			Map<String, String> attributes = recordChangedMessages.getAttributes();
			if (attributes == null || attributes.size() == 0) {
				// logger.warn("attribute map not found");
				// throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid record change
				// message", "attribute map not found", String.format("PubSub message: %s",
				// recordChangedMessages));
				attributes = new HashMap<String, String>();
				JsonObject jsonObjectMessage = jsonMessage.getAsJsonObject();
				attributes.put(DpsHeaders.DATA_PARTITION_ID,
						jsonObjectMessage.get(DpsHeaders.DATA_PARTITION_ID).getAsString());
				attributes.put(DpsHeaders.CORRELATION_ID,
						jsonObjectMessage.get(DpsHeaders.CORRELATION_ID).getAsString());
				recordChangedMessages.setAttributes(attributes);
			}
			Map<String, String> lowerCase = new HashMap<>();
			attributes.forEach((key, value) -> lowerCase.put(key.toLowerCase(), value));
			recordChangedMessages.setAttributes(lowerCase);
			if (recordChangedMessages.missingAccountId()) {
				logger.warn("tenant-id missing");
				throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid tenant", "tenant-id missing",
						String.format("PubSub message: %s", recordChangedMessages));
			}

			return recordChangedMessages;

		} catch (JsonParseException e) {
			logger.warn("Unable to parse request payload.", e);
			throw new AppException(HttpStatus.SC_BAD_REQUEST, "Request payload parsing error",
					"Unable to parse request payload.", e);
		}
	}

}
