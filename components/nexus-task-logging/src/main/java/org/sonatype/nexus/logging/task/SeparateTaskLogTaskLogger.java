/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.logging.task;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.MDC;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.NEXUS_LOG_ONLY;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.TASK_LOG_ONLY;

/**
 * {@link TaskLogger} implementation which handles the logic for creating separate task log files per task execution.
 * Extends {@link ProgressTaskLogger} to also include progress functionality.
 * Note logback handles most of the work (see logback.xml, TaskLogsFilter, and NexusLogFilter in nexus-pax-logging).
 *
 * @since 3.5
 */
public class SeparateTaskLogTaskLogger
    extends ProgressTaskLogger
{
  private final TaskLogInfo taskLogInfo;

  private final String taskLogIdentifier;

  SeparateTaskLogTaskLogger(final Logger log, final TaskLogInfo taskLogInfo) {
    super(log);
    this.taskLogInfo = checkNotNull(taskLogInfo);

    // Set per-thread logback property via MDC (see logback.xml)
    taskLogIdentifier = format("%s-%s", taskLogInfo.getTypeId(),
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
    MDC.put(LOGBACK_TASK_DISCRIMINATOR_ID, taskLogIdentifier);
  }

  private void logTaskInfo() {
    // dump task details to task log
    log.info(TASK_LOG_ONLY, "Task information:");
    log.info(TASK_LOG_ONLY, " ID: {}", taskLogInfo.getId());
    log.info(TASK_LOG_ONLY, " Type: {}", taskLogInfo.getTypeId());
    log.info(TASK_LOG_ONLY, " Name: {}", taskLogInfo.getName());
    log.info(TASK_LOG_ONLY, " Description: {}", taskLogInfo.getMessage());
    log.debug(TASK_LOG_ONLY, "Task configuration: {}", taskLogInfo.toString());

    String taskLogsHome = TaskLogHome.getTaskLogHome();
    if (taskLogsHome != null) {
      String filename = format("%s/%s.log", taskLogsHome, taskLogIdentifier);
      log.info(NEXUS_LOG_ONLY, "Task log: " + filename);
    }
  }

  @Override
  public final void start() {
    super.start();
    logTaskInfo();
  }

  @Override
  public final void finish() {
    super.finish();
    log.info(TASK_LOG_ONLY, "Task complete");
    MDC.remove(LOGBACK_TASK_DISCRIMINATOR_ID);
    MDC.remove(TASK_LOG_ONLY_MDC);
    MDC.remove(TASK_LOG_WITH_PROGRESS_MDC);
  }
}