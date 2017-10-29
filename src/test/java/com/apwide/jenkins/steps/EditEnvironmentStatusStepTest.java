package com.apwide.jenkins.steps;

import static com.apwide.jenkins.api.ResponseData.toResponseData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.model.Run;

import java.io.IOException;
import java.io.PrintStream;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.thoughtslive.jenkins.plugins.jira.Site;
import org.thoughtslive.jenkins.plugins.jira.api.ResponseData.ResponseDataBuilder;

import com.apwide.jenkins.api.ResponseData;
import com.apwide.jenkins.service.ApwideService;
import com.apwide.jenkins.util.ApwideSite;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ EditEnvironmentStatusStep.class, Site.class, ApwideSite.class })
public class EditEnvironmentStatusStepTest {

    @Mock
    TaskListener taskListenerMock;
    @Mock
    Run<?, ?> runMock;
    @Mock
    EnvVars envVarsMock;
    @Mock
    PrintStream printStreamMock;
    @Mock
    ApwideService apwideServiceMock;
    @Mock
    Site siteMock;
    @Mock
    ApwideSite apwideSiteMock;
    @Mock
    StepContext contextMock;

    @Before
    public void setup() throws IOException, InterruptedException {

	// Prepare site.
	when(envVarsMock.get("JIRA_SITE")).thenReturn("LOCAL");
	when(envVarsMock.get("BUILD_URL")).thenReturn("http://localhost:8080/jira-testing/job/01");

	PowerMockito.mockStatic(Site.class);
	Mockito.when(Site.get("LOCAL")).thenReturn(siteMock);
	PowerMockito.mockStatic(ApwideSite.class);
	Mockito.when(ApwideSite.get("LOCAL")).thenReturn(apwideSiteMock);
	when(apwideSiteMock.getApwideService()).thenReturn(apwideServiceMock);

	when(runMock.getCauses()).thenReturn(null);
	when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
	doNothing().when(printStreamMock).println();

	final ResponseDataBuilder<Void> builder = ResponseData.builder();
	when(apwideServiceMock.updateEnvironmentStatus(anyString(), anyString(), anyString(), anyString())).thenReturn(
		toResponseData(builder.successful(true).code(200).message("Success").build()));

	when(contextMock.get(Run.class)).thenReturn(runMock);
	when(contextMock.get(TaskListener.class)).thenReturn(taskListenerMock);
	when(contextMock.get(EnvVars.class)).thenReturn(envVarsMock);
    }

    protected void assertParamError(String paramName, EditEnvironmentStatusStep step) throws IOException, InterruptedException {
	EditEnvironmentStatusStep.Execution stepExecution = new EditEnvironmentStatusStep.Execution(step, contextMock);
	String message = "Param " + paramName + " cannot be null or empty!";
	assertThatExceptionOfType(AbortException.class).isThrownBy(() -> {
	    stepExecution.run();
	}).withMessage(message).withStackTraceContaining("AbortException").withNoCause();
    }

    @Test
    public void testWithBadParamsThrowsAbortException() throws Exception {
	EditEnvironmentStatusStep step;

	step = new EditEnvironmentStatusStep("", "Staging", "test comment", null);
	assertParamError("applicationName", step);

	step = new EditEnvironmentStatusStep(null, "Staging", "test comment", null);
	assertParamError("applicationName", step);

	step = new EditEnvironmentStatusStep("eCommerce", "", "test comment", null);
	assertParamError("categoryName", step);

	step = new EditEnvironmentStatusStep("eCommerce", null, "test comment", null);
	assertParamError("categoryName", step);
    }

    @Test
    public void testSuccessfulEditComment() throws Exception {
	EditEnvironmentStatusStep.Execution stepExecution;
	EditEnvironmentStatusStep step = new EditEnvironmentStatusStep("eCommerce", "Staging", "", null);
	stepExecution = new EditEnvironmentStatusStep.Execution(step, contextMock);
	stepExecution.run();
	verify(apwideServiceMock, times(1)).updateEnvironmentStatus("eCommerce", "Staging", "", null);
	assertThat(step.isFailOnError()).isEqualTo(true);

	final ResponseDataBuilder<Void> builder = ResponseData.builder();
	when(apwideServiceMock.updateEnvironmentStatus(anyString(), anyString(), anyString(), anyString())).thenReturn(
		toResponseData(builder.successful(true).code(302).message("Not Modified").build()));

	step = new EditEnvironmentStatusStep("eCommerce", "Staging", "", null);
	stepExecution = new EditEnvironmentStatusStep.Execution(step, contextMock);
	stepExecution.run();
	verify(apwideServiceMock, times(2)).updateEnvironmentStatus("eCommerce", "Staging", "", null);
	assertThat(step.isFailOnError()).isEqualTo(true);

    }
}
