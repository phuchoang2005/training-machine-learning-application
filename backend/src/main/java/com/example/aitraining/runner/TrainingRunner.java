package com.example.aitraining.runner;

import com.example.aitraining.domain.Models.Project;
import com.example.aitraining.domain.Models.TrainingJob;

/**
 * <b>Strategy Pattern</b> — defines the contract for a pluggable training execution engine.
 *
 * <p>The context ({@link com.example.aitraining.service.JobDispatcherService}) dispatches a job
 * to whichever concrete strategy is in the Spring context.  The concrete strategies extend
 * {@link AbstractTrainingRunner} and only need to implement the engine-specific
 * {@link AbstractTrainingRunner#execute} hook.
 *
 * <p>Current implementations:
 * <ul>
 *   <li>{@link DockerTrainingRunner} — runs the training process inside a Docker container.
 *       (active; covers the local single-server topology)</li>
 * </ul>
 *
 * <p>Planned implementations (swap by replacing the {@code @Component}-annotated bean):
 * <ul>
 *   <li>{@code KubernetesTrainingRunner} — submits a Pod via the K8s API (future scalability)</li>
 *   <li>{@code LocalProcessTrainingRunner} — runs directly on the host process (integration
 *       testing, no Docker dependency)</li>
 * </ul>
 *
 * <p>Implementations <em>must</em> be safe to call from multiple threads concurrently, because
 * the dispatcher submits jobs to a thread pool.
 *
 * @see AbstractTrainingRunner
 * @see DockerTrainingRunner
 */
public interface TrainingRunner {

  /**
   * Executes the training job end-to-end: workspace setup, training process, artifact
   * collection, and notifications.
   *
   * <p>Implementations must not throw; all errors should be caught internally and used to
   * transition the job to {@code FAILED}.
   *
   * @param job     the training job to execute; its status must be {@code RUNNING} on entry
   * @param project the owning project, used for source resolution and notification delivery
   */
  void run(TrainingJob job, Project project);
}
