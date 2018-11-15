// Copyright 2018 Signal Digital, Inc. All rights reserved.
package com.onetag.plugins;

import java.util.regex.Matcher;

import org.junit.Test;

import static com.onetag.plugins.TriggerStepFunctionBuilder.BUILD_VARIABLE_REGEX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Tony Gallotta
 * @since 11/14/2018
 */
public class TriggerStepFunctionBuilderTest {

  @Test
  public void variableRegex_noBraces() {
    Matcher matcher = BUILD_VARIABLE_REGEX.matcher("$PAYLOAD");
    assertTrue(matcher.matches());
    assertEquals("PAYLOAD", matcher.group(2));
  }

  @Test
  public void variableRegex_braces() {
    Matcher matcher = BUILD_VARIABLE_REGEX.matcher("${PAYLOAD}");
    assertTrue(matcher.matches());
    assertEquals("PAYLOAD", matcher.group(2));
  }
}
