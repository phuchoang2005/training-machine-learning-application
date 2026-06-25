package com.example.aitraining;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the AI Training Backend.
 *
 * <p>{@code @EnableScheduling} activates the {@link org.springframework.scheduling.annotation.Scheduled}
 * annotation used by {@link com.example.aitraining.service.JobDispatcherService#dispatch()}.
 */
@SpringBootApplication
@EnableScheduling
public class AiTrainingBackendApplication {

  /**
   * Starts the embedded Tomcat server on the configured port (default {@code 8080}).
   */
  public static void main(String[] args) {
    SpringApplication.run(AiTrainingBackendApplication.class, args);
  }
}
