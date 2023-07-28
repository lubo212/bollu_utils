package com.bollu.goosefs.config;

public enum ConsistencyCheckLevel {
  /**
   * Do not check consistency of property value.
   * This should be set if the property is not required to have same value among the nodes
   * (e.g. worker hostname).
   */
  IGNORE,
  /**
   * Check consistency of property value, show warning of inconsistent values.
   * This should be set if the property is recommended to have same value among the nodes,
   * although having different values does not cause immediate issues(e.g. goosefs home folder
   * location, timeout value for connecting to a worker).
   */
  WARN,
  /**
   * Check consistency of property value, show error of inconsistent values.
   * This should be set if the property is required to have same value among the nodes
   * (e.g. AWS credentials, journal location).
   */
  ENFORCE,
}
