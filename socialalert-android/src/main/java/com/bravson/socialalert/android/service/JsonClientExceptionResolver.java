package com.bravson.socialalert.android.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.ExceptionResolver;
import com.googlecode.jsonrpc4j.JsonRpcClientException;

public class JsonClientExceptionResolver implements ExceptionResolver {
	
	public Throwable resolveException(ObjectNode response) {
		// get the error object
		ObjectNode errorObject = ObjectNode.class.cast(response.get("error"));

		return new JsonRpcClientException(
				errorObject.get("code").asInt(),
				errorObject.get("message").asText(),
				errorObject.get("data"));
	}
}
