package com.example.aitraining.config;

import org.bson.UuidRepresentation;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Forces the standard UUID BSON representation so that {@code UUID} document ids round-trip
 * consistently. Spring Boot's auto-config otherwise leaves the driver at {@code UNSPECIFIED},
 * which rejects encoding UUID values.
 */
@Configuration
public class MongoConfig {

  @Bean
  MongoClientSettingsBuilderCustomizer uuidRepresentationCustomizer() {
    return builder -> builder.uuidRepresentation(UuidRepresentation.STANDARD);
  }
}
