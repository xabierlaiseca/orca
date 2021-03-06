/*
 * Copyright 2017 Schibsted ASA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.webhook.pipeline

import com.netflix.spinnaker.orca.CancellableStage
import com.netflix.spinnaker.orca.pipeline.StageDefinitionBuilder
import com.netflix.spinnaker.orca.pipeline.TaskNode
import com.netflix.spinnaker.orca.pipeline.model.Stage
import com.netflix.spinnaker.orca.pipeline.tasks.WaitTask
import com.netflix.spinnaker.orca.webhook.tasks.CreateWebhookTask
import com.netflix.spinnaker.orca.webhook.tasks.MonitorWebhookTask
import com.netflix.spinnaker.orca.pipeline.tasks.artifacts.BindProducedArtifactsTask
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

@Slf4j
@Component
class WebhookStage implements StageDefinitionBuilder, CancellableStage {

  @Override
  void taskGraph(Stage stage, TaskNode.Builder builder) {
    StageData stageData = stage.mapTo(StageData)

    builder.withTask("createWebhook", CreateWebhookTask)
    if (stageData.waitForCompletion) {
      if (stageData.waitBeforeMonitor > 0) {
        stage.context.putIfAbsent("waitTime", stageData.waitBeforeMonitor)
        builder.withTask("waitBeforeMonitorWebhook", WaitTask)
      }

      builder.withTask("monitorWebhook", MonitorWebhookTask)
    }
    if (stage.context.containsKey("expectedArtifacts")) {
      builder.withTask(BindProducedArtifactsTask.TASK_NAME, BindProducedArtifactsTask.class)
    }
  }

  @Override
  CancellableStage.Result cancel(Stage stage) {
    log.info("Cancelling stage (stageId: ${stage.id}, executionId: ${stage.execution.id}, context: ${stage.context as Map})")
    return new CancellableStage.Result(stage, [:])
  }

  static class StageData {
    boolean waitForCompletion
    int waitBeforeMonitor
  }
}
