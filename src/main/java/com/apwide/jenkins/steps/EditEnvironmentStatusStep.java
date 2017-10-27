package com.apwide.jenkins.steps;

import static com.apwide.jenkins.util.ApwideStepChecker.checkNotNull;
import static org.thoughtslive.jenkins.plugins.jira.util.Common.buildErrorResponse;
import hudson.Extension;

import java.io.IOException;

import lombok.Getter;
import lombok.ToString;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.thoughtslive.jenkins.plugins.jira.api.ResponseData;
import org.thoughtslive.jenkins.plugins.jira.steps.BasicJiraStep;
import org.thoughtslive.jenkins.plugins.jira.util.JiraStepDescriptorImpl;

import static org.thoughtslive.jenkins.plugins.jira.util.Common.log;
import com.apwide.jenkins.util.ApwideStepExecution;

/**
 * Step to update given JIRA issue.
 * 
 * @author Naresh Rayapati
 *
 */
@ToString(of = { "applicationName", "categoryName", "statusName", "statusId" })
public class EditEnvironmentStatusStep extends BasicJiraStep {

    private static final long serialVersionUID = -5047755533376456765L;

    @Getter
    private final String applicationName;

    @Getter
    private final String categoryName;

    @Getter
    private final String statusName;

    @Getter
    private final String statusId;

    @DataBoundConstructor
    public EditEnvironmentStatusStep(final String applicationName, final String categoryName, final String statusId, final String statusName) {
	this.applicationName = applicationName;
	this.categoryName = categoryName;
	this.statusName = statusName;
	this.statusId = statusId;
    }

    @Extension
    public static class DescriptorImpl extends JiraStepDescriptorImpl {

	@Override
	public String getFunctionName() {
	    return "apwideUpdateEnvironmentStatus";
	}

	@Override
	public String getDisplayName() {
	    return getPrefix() + "Update Environment Status";
	}

	@Override
	public boolean isMetaStep() {
	    return true;
	}
    }

    public static class Execution extends ApwideStepExecution<ResponseData<Object>> {

	private static final long serialVersionUID = -4127725325057889625L;

	private final EditEnvironmentStatusStep step;

	protected Execution(final EditEnvironmentStatusStep step, final StepContext context) throws IOException, InterruptedException {
	    super(context);
	    this.step = step;
	}

	@Override
	protected ResponseData<Object> run() throws Exception {

	    ResponseData<Object> response = verifyInput();

	    if (response == null) {
		log(logger, "APWIDE: Jira Site - " + siteName + " - Updating environment status: " + step);
		response = apwideService.updateEnvironmentStatus(step.getApplicationName(), step.getCategoryName(), step.getStatusId(),
			step.getStatusName());
	    }

	    return logResponse(response);
	}

	@Override
	protected <T> ResponseData<T> verifyInput() throws Exception {

	    ResponseData<T> errorResponse = verifyCommon(step);

	    if (errorResponse != null)
		return errorResponse;

	    try {

		checkNotNull(step.getApplicationName(), "applicationName");
		checkNotNull(step.getCategoryName(), "categoryName");
		return null;

	    } catch (Exception e) {
		return logResponse(buildErrorResponse(e));
	    }
	}
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
	return new Execution(this, context);
    }
}
