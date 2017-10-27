package com.apwide.jenkins.util;

import static org.thoughtslive.jenkins.plugins.jira.util.Common.log;
import hudson.AbortException;

import java.io.IOException;
import java.io.PrintStream;

import org.thoughtslive.jenkins.plugins.jira.api.ResponseData;
import org.thoughtslive.jenkins.plugins.jira.api.ResponseData.ResponseDataBuilder;

import retrofit2.Response;

public class ApwideResponseHandler {
    
    private static boolean isSuccessfulStatus(int status) {
	return (status >= 200 && status <= 399);
    }

    public static <T> ResponseData<T> parseResponse(final Response<T> response) throws IOException {
	final ResponseDataBuilder<T> builder = ResponseData.builder();
	builder.successful(isSuccessfulStatus(response.code())).code(response.code()).message(response.message());
	if (!response.isSuccessful()) {
	    final String errorMessage = response.errorBody().string();
	    builder.error(errorMessage);
	} else {
	    builder.data(response.body());
	}
	return builder.build();
    }

    public static <T> ResponseData<T> logResponse(ResponseData<T> response, PrintStream logger, boolean failOnError) throws AbortException {

	if (isSuccessfulStatus(response.getCode())) {
	    log(logger, "Successful. Code: " + response.getCode());
	} else {
	    log(logger, "Error Code: " + response.getCode());
	    log(logger, "Error Message: " + response.getError());

	    if (failOnError) {
		throw new AbortException(response.getError());
	    }
	}

	return response;
    }

}
