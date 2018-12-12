package com.onetag.plugins.tasks;

import java.io.PrintStream;
import java.time.Duration;

import javax.annotation.Nullable;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import hudson.util.VariableResolver;
import net.sf.json.JSONObject;

import com.onetag.plugins.model.InvokeStepFunctionConfig;
import com.onetag.plugins.model.InvokeStepFunctionResult;
import com.onetag.plugins.util.Sleeper;
import com.onetag.plugins.util.ThreadSleeper;

/**
 * A build step that invokes an AWS Step Function, awaits completion, logs the output, and passes if
 * the Step Function completed successfully.
 *
 * @author Tony Gallotta
 * @since 11/13/2018
 */
public class InvokeStepFunctionBuilder extends Builder {

  private final Sleeper sleeper;
  private final boolean useInstanceCredentials;
  // These properties may include build variables and must be interpolated
  private final String awsSecretKey;
  private final String awsAccessKeyId;
  private final String awsRegion;
  private final String stateMachineArn;
  private final String pollIntervalSeconds;
  private final String payload;

  @DataBoundConstructor
  public InvokeStepFunctionBuilder(boolean useInstanceCredentials, @Nullable String awsAccessKeyId,
      @Nullable String awsSecretKey, @Nullable String awsRegion, String stateMachineArn,
      String pollIntervalSeconds, String payload) {
    this.sleeper = new ThreadSleeper();
    this.useInstanceCredentials = useInstanceCredentials;
    this.awsAccessKeyId = Strings.nullToEmpty(awsAccessKeyId);
    this.awsSecretKey = Strings.nullToEmpty(awsSecretKey);
    this.awsRegion = Strings.nullToEmpty(awsRegion);
    this.stateMachineArn = stateMachineArn;
    this.pollIntervalSeconds = pollIntervalSeconds;
    this.payload = payload;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
    InvokeStepFunctionConfig config = buildConfig(build.getBuildVariableResolver());
    InvokeStepFunctionService invoker = createService(createStepFunctionClient(config),
        listener.getLogger(), config);
    InvokeStepFunctionResult result = invoker.invoke();
    return result.isSuccess();
  }

  /**
   * Creates the final configuration for an execution of the build, interpolating any parameters
   * in the configuration.
   */
  @VisibleForTesting InvokeStepFunctionConfig buildConfig(VariableResolver<String> variableResolver) {
    InvokeStepFunctionConfig.Builder builder = InvokeStepFunctionConfig.builder()
        .awsAccessKeyId(Util.replaceMacro(awsAccessKeyId, variableResolver))
        .awsSecretKey(Util.replaceMacro(awsSecretKey, variableResolver))
        .awsRegion(Util.replaceMacro(awsRegion, variableResolver))
        .stateMachineArn(Util.replaceMacro(stateMachineArn, variableResolver))
        .payload(Util.replaceMacro(payload, variableResolver));
    if (!Strings.isNullOrEmpty(pollIntervalSeconds)) {
      builder.pollInterval(Duration.ofSeconds(Long.valueOf(Util.replaceMacro(pollIntervalSeconds,
          variableResolver))));
    }
    return builder.build();
  }

  // Just so we can mock out this call in tests
  @VisibleForTesting AWSStepFunctions createStepFunctionClient(InvokeStepFunctionConfig config) {
    AWSStepFunctionsClientBuilder builder = AWSStepFunctionsClientBuilder.standard()
        .withRegion(config.getAwsRegion());
    if (useInstanceCredentials) {
      builder.withCredentials(new DefaultAWSCredentialsProviderChain());
    } else {
      builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(config.getAwsAccessKeyId(),
          config.getAwsSecretKey())));
    }
    return builder.build();
  }

  // Just so we can mock out this call in tests
  @VisibleForTesting InvokeStepFunctionService createService(AWSStepFunctions stepFunctions,
      PrintStream logger, InvokeStepFunctionConfig config) {
    return new InvokeStepFunctionService(stepFunctions, config, logger, sleeper);
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

  public String getPollIntervalSeconds() {
    return pollIntervalSeconds;
  }

  public String getPayload() {
    return payload;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<Builder> {
    private boolean useInstanceCredentials;
    private String awsAccessKeyId;
    private String awsSecretKey;
    private String awsRegion;
    private String functionName;
    private String pollIntervalSeconds = "30";
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
