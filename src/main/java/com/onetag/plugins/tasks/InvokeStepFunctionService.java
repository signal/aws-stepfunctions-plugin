package com.onetag.plugins.tasks;

import java.io.PrintStream;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DescribeExecutionRequest;
import com.amazonaws.services.stepfunctions.model.DescribeExecutionResult;
import com.amazonaws.services.stepfunctions.model.ExecutionStatus;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import com.google.common.annotations.VisibleForTesting;

import com.onetag.plugins.model.InvokeStepFunctionResult;
import com.onetag.plugins.model.InvokeStepFunctionConfig;
import com.onetag.plugins.util.Sleeper;

/**
 * Invokes a Step Function for a result.
 *
 * @author Tony Gallotta
 * @since 11/17/2018
 */
public class InvokeStepFunctionService {

  private final AWSStepFunctions stepFunctions;
  private final InvokeStepFunctionConfig config;
  private final PrintStream log;
  private final Sleeper sleeper;

  /**
   * @param stepFunctions the AWS Step Function API client.
   * @param config the configuration for this job execution.
   * @param log a stream to log output to.
   * @param sleeper used to delay polling attempts for results.
   */
  public InvokeStepFunctionService(AWSStepFunctions stepFunctions, InvokeStepFunctionConfig config,
      PrintStream log, Sleeper sleeper) {
    this.stepFunctions = stepFunctions;
    this.config = config;
    this.log = log;
    this.sleeper = sleeper;
  }

  /**
   * Invokes the Step Function defined by the configuration this instance was constructed with.
   *
   * @return an object detailing the result of the invocation.
   */
  public InvokeStepFunctionResult invoke() {
    StartExecutionResult startExecutionResult = startExecution();
    String executionArn = startExecutionResult.getExecutionArn();
    log.println("Started execution with ARN: " + executionArn);
    DescribeExecutionResult result = awaitCompletion(executionArn);
    return InvokeStepFunctionResult.builder()
        .executionArn(executionArn)
        .output(result.getOutput())
        .success(result.getStatus().equals(ExecutionStatus.SUCCEEDED.name()))
        .build();
  }

  private StartExecutionResult startExecution() {
    log.println(String.format("Invoking Step Function %s with payload %s", config.getStateMachineArn(),
        config.getPayload()));
    StartExecutionRequest request = new StartExecutionRequest()
        .withStateMachineArn(config.getStateMachineArn())
        .withInput(config.getPayload());
    return stepFunctions.startExecution(request);
  }

  @VisibleForTesting DescribeExecutionResult awaitCompletion(String executionArn) {
    DescribeExecutionResult result = stepFunctions.describeExecution(new DescribeExecutionRequest()
        .withExecutionArn(executionArn));
    while (ExecutionStatus.RUNNING.name().equals(result.getStatus())) {
      try {
        log.println("Function still executing, sleeping for " + config.getPollInterval());
        sleeper.sleep(config.getPollInterval().toMillis());
      } catch (InterruptedException e) {
        // ignore
      }
      result = stepFunctions.describeExecution(new DescribeExecutionRequest().withExecutionArn(executionArn));
    }
    log.println("Final execution status: " + result.getStatus());
    log.println("Output: " + result.getOutput());
    return result;
  }
}
