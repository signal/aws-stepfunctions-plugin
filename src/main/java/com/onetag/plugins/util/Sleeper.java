package com.onetag.plugins.util;

/**
 * An abstraction to {@link Thread#sleep(long)} so we can mock it out in tests.
 *
 * @author Tony Gallotta
 * @since 11/17/2018
 */
public interface Sleeper {

  /**
   * Causes the currently executing thread to sleep (temporarily cease execution).
   *
   * @param millis the number of milliseconds to sleep for.
   * @throws InterruptedException if the thread was interrupted.
   */
  void sleep(long millis) throws InterruptedException;
}
