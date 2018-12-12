package com.onetag.plugins.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Simplified (compared to the SDK class) representation of the result of an AWS Step Function
 * execution.
 *
 * @author Tony Gallotta
 * @since 11/17/2018
 */
public class InvokeStepFunctionResult {

  private final String output;
  private final String executionArn;
  private final boolean success;

  private InvokeStepFunctionResult(Builder builder) {
    this.output = builder.output;
    this.executionArn = builder.executionArn;
    this.success = builder.success;
  }

  /**
   * @return the final output of the Step Function execution.
   */
  public String getOutput() {
    return output;
  }

  /**
   * @return the ARN of the Step Function execution, for example:
   * "arn:aws:states:us-east-1:123456789012:execution:my_step_function:execution-id"
   */
  public String getExecutionArn() {
    return executionArn;
  }

  /**
   * @return {@code true} if the Step Function completed successfully, {@code false} otherwise.
   */
  public boolean isSuccess() {
    return success;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InvokeStepFunctionResult that = (InvokeStepFunctionResult)o;
    return success == that.success &&
        Objects.equal(output, that.output) &&
        Objects.equal(executionArn, that.executionArn);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(output, executionArn, success);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("output", output)
        .add("executionArn", executionArn)
        .add("success", success)
        .toString();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder builder(InvokeStepFunctionResult prototype) {
    return new Builder().fromPrototype(prototype);
  }

  public static class Builder {

    private String output;
    private String executionArn;
    private boolean success;

    private Builder() {}

    public Builder fromPrototype(InvokeStepFunctionResult prototype) {
      this.output = prototype.output;
      this.executionArn = prototype.executionArn;
      this.success = prototype.success;
      return this;
    }

    public Builder output(String output) {
      this.output = output;
      return this;
    }

    public Builder executionArn(String executionArn) {
      this.executionArn = executionArn;
      return this;
    }

    public Builder success(boolean success) {
      this.success = success;
      return this;
    }

    public InvokeStepFunctionResult build() {
      return new InvokeStepFunctionResult(this);
    }
  }
}
