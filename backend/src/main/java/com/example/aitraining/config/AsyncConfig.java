package com.example.aitraining.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configures the {@code trainingExecutor} thread pool used by
 * {@link com.example.aitraining.service.JobDispatcherService} to run training jobs
 * concurrently.
 *
 * <p>Pool sizing:
 * <ul>
 *   <li>Core pool: 4 threads — always alive, ready to accept jobs.</li>
 *   <li>Max pool: 10 threads — burst capacity beyond the core pool.</li>
 *   <li>Queue: 50 tasks — bounded to prevent unbounded memory growth if the training
 *       runner backs up.</li>
 * </ul>
 * Graceful shutdown waits up to 30 seconds for in-flight jobs to complete before the
 * JVM exits.
 */
@Configuration
public class AsyncConfig {

  /**
   * Thread pool executor for training job execution.
   * Injected into {@link com.example.aitraining.service.JobDispatcherService} via
   * {@code @Qualifier("trainingExecutor")}.
   */
  @Bean(name = "trainingExecutor")
  public TaskExecutor trainingExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("training-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.initialize();
    return executor;
  }
}
