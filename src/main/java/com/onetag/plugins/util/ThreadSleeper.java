package com.onetag.plugins.util;

/**
 * The "real" sleeper, that just dispatches to Thread.sleep().
 *
 * @author Tony Gallotta
 * @since 11/17/2018
 */
public class ThreadSleeper implements Sleeper {

  @Override
  public void sleep(long millis) throws InterruptedException {
    Thread.sleep(millis);
  }
}
