package com.apwide.jenkins.builders;

import static org.thoughtslive.jenkins.plugins.jira.util.Common.empty;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Cause.UserIdCause;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.tasks.SimpleBuildStep;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.apwide.jenkins.util.ApwideSite;

public class ApwideEnvironmentUpdateBuilder extends Builder implements SimpleBuildStep {
    private final String environmentId;
    private final String statusId;

    protected boolean failOnError = true;
    protected String buildUser;
    protected String buildUrl;

    @DataBoundConstructor
    public ApwideEnvironmentUpdateBuilder(String environmentId, String statusId) {
	this.environmentId = Util.fixEmptyAndTrim(environmentId);
	this.statusId = Util.fixEmptyAndTrim(statusId);
    }

    public String getEnvironmentId() {
	return environmentId;
    }

    public String getStatusId() {
	return statusId;
    }

    protected String prepareBuildUser(List<Cause> causes) {
	String buildUser = "anonymous";
	if (causes != null && causes.size() > 0) {
	    if (causes.get(0) instanceof UserIdCause) {
		buildUser = ((UserIdCause) causes.get(0)).getUserName();
	    } else if (causes.get(0) instanceof UpstreamCause) {
		List<Cause> upstreamCauses = ((UpstreamCause) causes.get(0)).getUpstreamCauses();
		prepareBuildUser(upstreamCauses);
	    }
	}
	return buildUser;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

	String realEnvironmentId = Util.fixEmptyAndTrim(run.getEnvironment(listener).expand(environmentId));
	String realStatusId = Util.fixEmptyAndTrim(run.getEnvironment(listener).expand(statusId));

	final String failOnErrorStr = Util.fixEmpty(run.getEnvironment(listener).get("JIRA_FAIL_ON_ERROR"));

	if (failOnErrorStr != null) {
	    failOnError = Boolean.parseBoolean(failOnErrorStr);
	}

	String siteName = run.getEnvironment(listener).get("JIRA_SITE");
	if (empty(siteName)) {
	    listener.getLogger().println("APWIDE: environment variable JIRA_SITE is empty or null.");
	    if (failOnError)
		run.setResult(Result.FAILURE);
	    return;
	}

	ApwideSite site = ApwideSite.get(siteName);
	if (site == null) {
	    listener.getLogger().println("APWIDE: no JIRA site configured with " + siteName + " name.");
	    if (failOnError)
		run.setResult(Result.FAILURE);
	    return;
	}

	buildUser = prepareBuildUser(run.getCauses());
	buildUrl = run.getEnvironment(listener).get("BUILD_URL");

	// TODO check params + do the job

    }

    @Override
    public DescriptorImpl getDescriptor() {
	return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

	public FormValidation doCheckEnvironmentId(@QueryParameter String value) throws IOException, ServletException {
	    if (value.length() == 0) {
		return FormValidation.error("Environment cannot be null!");// Messages.JiraIssueUpdateBuilder_NoJqlSearch());
	    }
	    return FormValidation.ok();
	}

	public FormValidation doCheckStatusId(@QueryParameter String value) throws IOException, ServletException {
	    return FormValidation.ok();
	}

	public boolean isApplicable(Class<? extends AbstractProject> klass) {
	    return true;
	}

	public String getDisplayName() {
	    // return Messages.JiraIssueUpdateBuilder_DisplayName();
	    return "Apwide environments";
	}
    }
}
