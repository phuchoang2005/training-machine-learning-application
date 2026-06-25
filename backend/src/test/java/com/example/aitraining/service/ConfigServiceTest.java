package com.example.aitraining.service;

import com.example.aitraining.dto.ProjectDtos.ValidateYamlResponse;
import com.example.aitraining.repo.ConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

  @Mock
  ConfigRepository configRepository;

  ConfigService service;

  @BeforeEach
  void setUp() {
    service = new ConfigService(configRepository);
  }

  @Test
  void validYamlReturnsValid() {
    ValidateYamlResponse response = service.validate("""
        trainingEntrypoint: train.py
        hyperparameters:
          epochs: 10
          lr: 0.001
        """);
    assertThat(response.valid()).isTrue();
    assertThat(response.errors()).isEmpty();
    assertThat(response.normalizedPreview()).containsKey("trainingEntrypoint");
  }

  @Test
  void invalidYamlReturnsErrors() {
    ValidateYamlResponse response = service.validate("key: [\nbad yaml");
    assertThat(response.valid()).isFalse();
    assertThat(response.errors()).isNotEmpty();
    assertThat(response.normalizedPreview()).isEmpty();
  }

  @Test
  void emptyYamlIsValid() {
    ValidateYamlResponse response = service.validate("{}");
    assertThat(response.valid()).isTrue();
  }

  @Test
  void scalarYamlIsHandled() {
    ValidateYamlResponse response = service.validate("just a string");
    assertThat(response.valid()).isTrue();
    assertThat(response.normalizedPreview()).containsKey("value");
  }
}
