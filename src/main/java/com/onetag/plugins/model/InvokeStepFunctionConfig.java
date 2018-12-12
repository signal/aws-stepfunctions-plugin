package com.onetag.plugins.model;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * This class models the configuration provided for a specific job execution.
 *
 * @author Tony Gallotta
 * @since 11/17/2018
 */
public class InvokeStepFunctionConfig {

  private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(30);
  private final String awsAccessKeyId;
  private final String awsSecretKey;
  private final String awsRegion;
  private final String stateMachineArn;
  private final Duration pollInterval;
  private final String payload;

  @JsonCreator
  private InvokeStepFunctionConfig(Builder builder) {
    this.awsAccessKeyId = builder.awsAccessKeyId;
    this.awsSecretKey = builder.awsSecretKey;
    this.awsRegion = builder.awsRegion;
    this.stateMachineArn = builder.stateMachineArn;
    this.pollInterval = builder.pollInterval;
    this.payload = builder.payload;
  }

  public String getAwsAccessKeyId() {
    return awsAccessKeyId;
  }

  public String getAwsSecretKey() {
    return awsSecretKey;
  }

  /**
   * @return the AWS region the Step Function exists in.
   */
  public String getAwsRegion() {
    return awsRegion;
  }

  /**
   * @return the ARN of the state machine to invoke, for example:
   * "arn:aws:states:us-east-1:123456789012:stateMachine:my_step_function".
   */
  public String getStateMachineArn() {
    return stateMachineArn;
  }

  /**
   * @return the duration to wait between polls checking the current status of the state machine.
   */
  public Duration getPollInterval() {
    return pollInterval;
  }

  /**
   * @return the JSON payload to invoke the Step Function with.
   */
  public String getPayload() {
    return payload;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InvokeStepFunctionConfig config = (InvokeStepFunctionConfig)o;
    return Objects.equal(awsAccessKeyId, config.awsAccessKeyId) &&
        Objects.equal(awsSecretKey, config.awsSecretKey) &&
        Objects.equal(awsRegion, config.awsRegion) &&
        Objects.equal(stateMachineArn, config.stateMachineArn) &&
        Objects.equal(pollInterval, config.pollInterval) &&
        Objects.equal(payload, config.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(awsAccessKeyId, awsSecretKey, awsRegion, stateMachineArn, pollInterval, payload);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("awsAccessKeyId", awsAccessKeyId)
        .add("awsSecretKey", awsSecretKey)
        .add("awsRegion", awsRegion)
        .add("stateMachineArn", stateMachineArn)
        .add("pollInterval", pollInterval)
        .add("payload", payload)
        .toString();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder builder(InvokeStepFunctionConfig prototype) {
    return new Builder().fromPrototype(prototype);
  }

  public static class Builder {

    private String awsAccessKeyId = "";
    private String awsSecretKey = "";
    private String awsRegion = "";
    private String stateMachineArn;
    private Duration pollInterval = DEFAULT_POLL_INTERVAL;
    private String payload;

    private Builder() { }

    public Builder fromPrototype(InvokeStepFunctionConfig prototype) {
      this.awsAccessKeyId = prototype.awsAccessKeyId;
      this.awsSecretKey = prototype.awsSecretKey;
      this.awsRegion = prototype.awsRegion;
      this.stateMachineArn = prototype.stateMachineArn;
      this.pollInterval = prototype.pollInterval;
      this.payload = prototype.payload;
      return this;
    }

    public Builder awsAccessKeyId(String awsAccessKeyId) {
      this.awsAccessKeyId = awsAccessKeyId;
      return this;
    }

    public Builder awsSecretKey(String awsSecretKey) {
      this.awsSecretKey = awsSecretKey;
      return this;
    }

    public Builder awsRegion(String awsRegion) {
      this.awsRegion = awsRegion;
      return this;
    }

    public Builder stateMachineArn(String stateMachineArn) {
      this.stateMachineArn = stateMachineArn;
      return this;
    }

    public Builder pollInterval(Duration pollInterval) {
      this.pollInterval = pollInterval;
      return this;
    }

    public Builder payload(String payload) {
      this.payload = payload;
      return this;
    }

    public InvokeStepFunctionConfig build() {
      return new InvokeStepFunctionConfig(this);
    }
  }
}
