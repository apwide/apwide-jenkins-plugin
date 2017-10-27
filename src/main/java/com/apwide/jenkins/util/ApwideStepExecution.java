package com.apwide.jenkins.util;

import static org.thoughtslive.jenkins.plugins.jira.util.Common.buildErrorResponse;
import static org.thoughtslive.jenkins.plugins.jira.util.Common.log;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.model.Run;

import java.io.IOException;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.thoughtslive.jenkins.plugins.jira.api.ResponseData;
import org.thoughtslive.jenkins.plugins.jira.steps.BasicJiraStep;
import org.thoughtslive.jenkins.plugins.jira.util.JiraStepExecution;

import retrofit2.Response;

import com.apwide.jenkins.service.ApwideService;

public abstract class ApwideStepExecution<T> extends JiraStepExecution<T> {

    private static final long serialVersionUID = -4495525306914574228L;

    protected transient ApwideService apwideService = null;

    private transient Run<?, ?> run;
    private transient TaskListener listener;
    private transient EnvVars envVars;

    protected ApwideStepExecution(StepContext context) throws IOException, InterruptedException {
	super(context);
	run = context.get(Run.class);
	listener = context.get(TaskListener.class);
	envVars = context.get(EnvVars.class);
    }

    private <T> ResponseData<T> runtimeException(String message) {
	return buildErrorResponse(new RuntimeException(message));
    }

    /**
     * Verifies the common input for all the stesp.
     * 
     * @param step
     * @return response if JIRA_SITE is empty or if there is no site configured
     *         with JIRA_SITE.
     * @throws AbortException
     *             when failOnError is true and JIRA_SITE is missing.
     */
    @SuppressWarnings("hiding")
    protected <T> ResponseData<T> verifyCommon(final BasicJiraStep step) throws AbortException {
	super.verifyCommon(step);

	logger = listener.getLogger();

	final ApwideSite site = ApwideSite.get(siteName);

	if (site == null) {
	    return runtimeException("No APWIDE JIRA site configured with " + siteName + " name.");
	} else {

	    if (apwideService == null)
		apwideService = site.getApwideService();

	    if (apwideService == null)
		runtimeException("Problem during initialization of APWIDE service. Site name:" + siteName);

	    buildUser = prepareBuildUser(run.getCauses());
	    buildUrl = envVars.get("BUILD_URL");

	    return null;
	}
    }

    @Override
    protected <T> ResponseData<T> logResponse(ResponseData<T> response) throws AbortException {
	return ApwideResponseHandler.logResponse(response, logger, failOnError);
    }

}
