package com.onetag.plugins.tasks;

import java.io.PrintStream;
import java.time.Duration;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DescribeExecutionRequest;
import com.amazonaws.services.stepfunctions.model.DescribeExecutionResult;
import com.amazonaws.services.stepfunctions.model.ExecutionStatus;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.onetag.plugins.model.InvokeStepFunctionConfig;
import com.onetag.plugins.model.InvokeStepFunctionResult;
import com.onetag.plugins.util.Sleeper;

import static org.junit.Assert.assertEquals;

/**
 * @author Tony Gallotta
 * @since 11/21/2018
 */
@RunWith(MockitoJUnitRunner.class)
public class InvokeStepFunctionServiceTest extends Mockito {

  private static final String STEP_FUNCTION_ARN = "arn:aws:states:us-east-1:123456789012:stateMachine:my_step_function";
  private static final String EXECUTION_ARN = "arn:aws:states:us-east-1:123456789012:execution:my_step_function:execution-id";
  private static final DescribeExecutionRequest DESCRIBE_EXECUTION_REQUEST = new DescribeExecutionRequest()
      .withExecutionArn(EXECUTION_ARN);
  private static final String PAYLOAD = "{\"message\":\"hello!\"}";
  private static final InvokeStepFunctionConfig CONFIG = InvokeStepFunctionConfig.builder()
      .pollInterval(Duration.ofSeconds(60))
      .stateMachineArn(STEP_FUNCTION_ARN)
      .payload(PAYLOAD)
      .build();
  private static final DescribeExecutionResult RUNNING_RESULT = new DescribeExecutionResult()
      .withExecutionArn(EXECUTION_ARN)
      .withStatus(ExecutionStatus.RUNNING);
  private static final DescribeExecutionResult SUCCESSFUL_RESULT = new DescribeExecutionResult()
      .withExecutionArn(EXECUTION_ARN)
      .withStatus(ExecutionStatus.SUCCEEDED)
      .withOutput("some output");
  private @Mock AWSStepFunctions stepFunctions;
  private @Mock PrintStream log;
  private @Mock Sleeper sleeper;
  private InvokeStepFunctionService invoker;

  @Before
  public void setUp() {
    invoker = new InvokeStepFunctionService(stepFunctions, CONFIG, log, sleeper);
    StartExecutionRequest startExecutionRequest = new StartExecutionRequest()
        .withStateMachineArn(CONFIG.getStateMachineArn())
        .withInput(CONFIG.getPayload());
    when(stepFunctions.startExecution(startExecutionRequest))
        .thenReturn(new StartExecutionResult().withExecutionArn(EXECUTION_ARN));
  }

  @Test
  public void invoke_success() {
    when(stepFunctions.describeExecution(DESCRIBE_EXECUTION_REQUEST))
        .thenReturn(RUNNING_RESULT)
        .thenReturn(SUCCESSFUL_RESULT);
    InvokeStepFunctionResult expected = InvokeStepFunctionResult.builder()
        .executionArn(EXECUTION_ARN)
        .output("some output")
        .success(true)
        .build();
    assertEquals(expected, invoker.invoke());
    verify(stepFunctions, times(2)).describeExecution(DESCRIBE_EXECUTION_REQUEST);
  }

  @Test
  public void invoke_failure() {
    DescribeExecutionResult failureResult = new DescribeExecutionResult()
        .withExecutionArn(EXECUTION_ARN)
        .withStatus(ExecutionStatus.FAILED);
    when(stepFunctions.describeExecution(DESCRIBE_EXECUTION_REQUEST))
        .thenReturn(RUNNING_RESULT)
        .thenReturn(failureResult);
    InvokeStepFunctionResult expected = InvokeStepFunctionResult.builder()
        .executionArn(EXECUTION_ARN)
        .success(false)
        .build();
    assertEquals(expected, invoker.invoke());
    verify(stepFunctions, times(2)).describeExecution(DESCRIBE_EXECUTION_REQUEST);
  }

  @Test
  public void invoke_aborted() {
    DescribeExecutionResult failureResult = new DescribeExecutionResult()
        .withExecutionArn(EXECUTION_ARN)
        .withStatus(ExecutionStatus.ABORTED);
    when(stepFunctions.describeExecution(DESCRIBE_EXECUTION_REQUEST))
        .thenReturn(RUNNING_RESULT)
        .thenReturn(failureResult);
    InvokeStepFunctionResult expected = InvokeStepFunctionResult.builder()
        .executionArn(EXECUTION_ARN)
        .success(false)
        .build();
    assertEquals(expected, invoker.invoke());
    verify(stepFunctions, times(2)).describeExecution(DESCRIBE_EXECUTION_REQUEST);
  }

  @Test
  public void awaitCompletion() throws InterruptedException {
    when(stepFunctions.describeExecution(DESCRIBE_EXECUTION_REQUEST))
        .thenReturn(RUNNING_RESULT)
        .thenReturn(RUNNING_RESULT)
        .thenReturn(SUCCESSFUL_RESULT);
    invoker.awaitCompletion(EXECUTION_ARN);
    verify(sleeper, times(2)).sleep(60_000);
  }
}
