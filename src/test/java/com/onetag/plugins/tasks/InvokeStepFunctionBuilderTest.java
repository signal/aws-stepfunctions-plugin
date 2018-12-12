// Copyright 2018 Signal Digital, Inc. All rights reserved.
package com.onetag.plugins.tasks;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.Duration;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.VariableResolver;

import com.onetag.plugins.model.InvokeStepFunctionConfig;
import com.onetag.plugins.model.InvokeStepFunctionResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Tony Gallotta
 * @since 11/21/2018
 */
@RunWith(MockitoJUnitRunner.class)
public class InvokeStepFunctionBuilderTest extends Mockito {

  private static final String STEP_FUNCTION_ARN = "arn:aws:states:us-east-1:123456789012:stateMachine:my_step_function";
  private static final String PAYLOAD = "{\"message\":\"hello!\"}";
  private static final PrintStream STD_OUT = new PrintStream(new FileOutputStream(FileDescriptor.out));
  private static final InvokeStepFunctionConfig CONFIG = InvokeStepFunctionConfig.builder()
      .stateMachineArn(STEP_FUNCTION_ARN)
      .payload(PAYLOAD)
      .pollInterval(Duration.ofSeconds(10))
      .build();
  private @Mock AbstractBuild<?, ?> build;
  private @Mock Launcher launcher;
  private @Mock BuildListener buildListener;
  private @Mock AWSStepFunctions stepFunctions;
  private @Mock InvokeStepFunctionService service;
  private InvokeStepFunctionBuilder builder;

  @Before
  public void setUp() {
    when(build.getBuildVariables()).thenReturn(ImmutableMap.of());
    when(buildListener.getLogger()).thenReturn(STD_OUT);
    builder = spy(new InvokeStepFunctionBuilder(true, null, null, null, STEP_FUNCTION_ARN, "10", PAYLOAD));
    doReturn(stepFunctions).when(builder).createStepFunctionClient(any(InvokeStepFunctionConfig.class));
    doReturn(service).when(builder).createService(stepFunctions, STD_OUT, CONFIG);
    when(service.invoke()).thenReturn(InvokeStepFunctionResult.builder()
        .success(true)
        .build());
  }

  @Test
  public void perform_success() {
    assertTrue(builder.perform(build, launcher, buildListener));
  }

  @Test
  public void perform_failure() {
    when(service.invoke()).thenReturn(InvokeStepFunctionResult.builder()
        .success(false)
        .build());
    assertFalse(builder.perform(build, launcher, buildListener));
  }

  @Test
  public void buildConfig_noVariables() {
    assertEquals(CONFIG, builder.buildConfig(VariableResolver.NONE));
  }

  @Test
  public void buildConfig_variables() {
    builder = new InvokeStepFunctionBuilder(false, "${ACCESS_KEY}", "${SECRET}", "${REGION}",
        "${ARN}", "$POLL_INTERVAL", "{\"message\":\"${MESSAGE}!\"}");
    InvokeStepFunctionConfig expected = InvokeStepFunctionConfig.builder()
        .awsAccessKeyId("access-key")
        .awsSecretKey("shhh")
        .awsRegion("us-west-1")
        .stateMachineArn(STEP_FUNCTION_ARN)
        .payload(PAYLOAD)
        .pollInterval(Duration.ofSeconds(10))
        .build();
    ImmutableMap<String, String> variables = ImmutableMap.<String, String>builder()
      .put("ARN", STEP_FUNCTION_ARN)
      .put("ACCESS_KEY", "access-key")
      .put("SECRET", "shhh")
      .put("REGION", "us-west-1")
      .put("POLL_INTERVAL", "10")
      .put("MESSAGE", "hello")
      .build();
    assertEquals(expected, builder.buildConfig(variables::get));
  }
}
