package com.apwide.jenkins.service;

import static org.thoughtslive.jenkins.plugins.jira.util.Common.buildErrorResponse;

import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import org.thoughtslive.jenkins.plugins.jira.Site;
import org.thoughtslive.jenkins.plugins.jira.api.ResponseData;
import org.thoughtslive.jenkins.plugins.jira.login.SigningInterceptor;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import com.apwide.jenkins.api.EnvironmentStatus;
import com.apwide.jenkins.util.ApwideResponseHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public class ApwideService {

    private Site jiraSite;
    private ApwideEndPoints rest;

    public ApwideService(Site site) {
	this.jiraSite = site;

	final ConnectionPool CONNECTION_POOL = new ConnectionPool(5, 60, TimeUnit.SECONDS);

	OkHttpClient httpClient = new OkHttpClient.Builder().connectTimeout(jiraSite.getTimeout(), TimeUnit.MILLISECONDS)
		.readTimeout(10000, TimeUnit.MILLISECONDS).connectionPool(CONNECTION_POOL).retryOnConnectionFailure(true)
		.addInterceptor(new SigningInterceptor(jiraSite)).build();

	final ObjectMapper mapper = new ObjectMapper();
	mapper.registerModule(new JodaModule());
	this.rest = new Retrofit.Builder().baseUrl(this.jiraSite.getUrl().toString()).addConverterFactory(JacksonConverterFactory.create(mapper))
		.addCallAdapterFactory(RxJavaCallAdapterFactory.create()).client(httpClient).build().create(ApwideEndPoints.class);
    }

    public ResponseData<Object> updateEnvironmentStatus(String applicationName, String categoryName, String statusId, String statusName) {
	try {
	    EnvironmentStatus status = new EnvironmentStatus(statusId, statusName);
	    Response<Object> resp = rest.updateStatus(applicationName, categoryName, status).execute();
	    return ApwideResponseHandler.parseResponse(resp);
	} catch (Exception e) {
	    return buildErrorResponse(e);
	}
    }
}
