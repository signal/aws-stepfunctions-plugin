// Copyright 2018 Signal Digital, Inc. All rights reserved.
package com.onetag.plugins;

import java.io.PrintStream;
import java.util.Map;
import java.util.regex.Pattern;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.DescribeExecutionRequest;
import com.amazonaws.services.stepfunctions.model.DescribeExecutionResult;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import com.google.common.annotations.VisibleForTesting;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.ParametersAction;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;

/**
 * @author Tony Gallotta
 * @since 11/13/2018
 */
public class TriggerStepFunctionBuilder extends Builder {

  private static final long DEFAULT_POLL_INTERVAL = 30_000;
  @VisibleForTesting static final Pattern BUILD_VARIABLE_REGEX = Pattern.compile("(\\$\\{?)(.*[^\\}])(\\}?)");
  private final boolean useInstanceCredentials;
  private final String awsAccessKeyId;
  private final String awsSecretKey;
  private final String awsRegion;
  private final String stateMachineArn;
  private final long pollIntervalSeconds;
  private final String payload;

  @DataBoundConstructor
  public TriggerStepFunctionBuilder(boolean useInstanceCredentials, String awsAccessKeyId,
      String awsSecretKey, String awsRegion, String stateMachineArn, long pollIntervalSeconds,
      String payload) {
    this.useInstanceCredentials = useInstanceCredentials;
    this.awsAccessKeyId = awsAccessKeyId;
    this.awsSecretKey = awsSecretKey;
    this.awsRegion = awsRegion;
    this.stateMachineArn = stateMachineArn;
    this.pollIntervalSeconds = pollIntervalSeconds;
    this.payload = payload;
  }


  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
    PrintStream resultLogger = listener.getLogger();
    build.getActions(ParametersAction.class);
    Map<String, String> buildVariables = build.getBuildVariables();
    resultLogger.println("BuildVariables : " + buildVariables);
    AWSStepFunctions stepFunctions = createStepFunctionClient();
    String payload = interpolateVariables(this.payload, buildVariables);
    String stateMachineArn = interpolateVariables(this.stateMachineArn, buildVariables);

    resultLogger.println("Invoking step function " + stateMachineArn + " with payload " + payload);
    StartExecutionResult startExecutionResult = startExecution(stepFunctions, stateMachineArn, payload);
    String executionArn = startExecutionResult.getExecutionArn();
    resultLogger.println("Started execution with ARN: " + executionArn);

    DescribeExecutionResult result = awaitCompletion(stepFunctions, executionArn, resultLogger);
    resultLogger.println("Final execution status: " + result.getStatus());
    resultLogger.println("Output: " + result.getOutput());
    return "SUCCEEDED".equals(result.getStatus());
  }

  private AWSStepFunctions createStepFunctionClient() {
    return AWSStepFunctionsClientBuilder.standard()
        .withRegion(awsRegion)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKeyId, awsSecretKey)))
        .build();
  }

  // No way this is the right way to do this
  private static String interpolateVariables(String pattern, Map<String, String> buildVariables) {
    String interpolated = pattern;
    for (Map.Entry<String, String> buildVariable : buildVariables.entrySet()) {
      interpolated = interpolated.replaceAll("\\$\\{?" + buildVariable.getKey() + "\\}?", buildVariable.getValue());
    }
    return interpolated;
  }

  private DescribeExecutionResult awaitCompletion(AWSStepFunctions stepFunctions, String executionArn, PrintStream resultLogger) {
    DescribeExecutionResult result = stepFunctions.describeExecution(new DescribeExecutionRequest().withExecutionArn(executionArn));
    while ("RUNNING".equals(result.getStatus())) {
      try {
        resultLogger.println("Execution status is " + result.getStatus());
        Thread.sleep(pollIntervalSeconds == 0 ? DEFAULT_POLL_INTERVAL : pollIntervalSeconds * 1000);
      } catch (InterruptedException e) {
        // ignore
      }
      result = stepFunctions.describeExecution(new DescribeExecutionRequest().withExecutionArn(executionArn));
    }
    return result;
  }

  private static StartExecutionResult startExecution(AWSStepFunctions stepFunctions,
      String stateMachineArn, String payload) {
    StartExecutionRequest request = new StartExecutionRequest()
        .withStateMachineArn(stateMachineArn)
        .withInput(payload);
    return stepFunctions.startExecution(request);
  }

  public boolean isUseInstanceCredentials() {
    return useInstanceCredentials;
  }

  public String getAwsAccessKeyId() {
    return awsAccessKeyId;
  }

  public String getAwsSecretKey() {
    return awsSecretKey;
  }

  public String getAwsRegion() {
    return awsRegion;
  }

  public String getStateMachineArn() {
    return stateMachineArn;
  }

  public long getPollIntervalSeconds() {
    return pollIntervalSeconds;
  }

  public String getPayload() {
    return payload;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<Builder> {
    private boolean useInstanceCredentials;
    private String awsAccessKeyId;
    private String awsSecretKey;
    private String awsRegion;
    private String functionName;
    private long pollIntervalSeconds;
    private String payload;

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
      req.bindJSON(this, json);
      save();
      return super.configure(req, json);
    }

    @Override
    public String getDisplayName() {
      return "AWS Step Function Invocation";
    }
  }
}
