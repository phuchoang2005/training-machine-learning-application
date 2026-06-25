package com.example.aitraining.runner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressParserTest {

  @Test
  void epochSlashFormat() {
    var result = ProgressParser.parse("Epoch 3/10 — loss: 0.42");
    assertThat(result).isPresent();
    assertThat(result.get().epoch()).isEqualTo(3);
    assertThat(result.get().totalEpoch()).isEqualTo(10);
    assertThat(result.get().value()).isEqualTo(30);
  }

  @Test
  void epochCaseInsensitive() {
    var result = ProgressParser.parse("EPOCH 5 / 20");
    assertThat(result).isPresent();
    assertThat(result.get().epoch()).isEqualTo(5);
    assertThat(result.get().totalEpoch()).isEqualTo(20);
    assertThat(result.get().value()).isEqualTo(25);
  }

  @Test
  void stepFormat() {
    var result = ProgressParser.parse("step 4 / 8 completed");
    assertThat(result).isPresent();
    assertThat(result.get().epoch()).isNull();
    assertThat(result.get().value()).isEqualTo(50);
  }

  @Test
  void barePercentage() {
    var result = ProgressParser.parse("Training: 75% done");
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo(75);
    assertThat(result.get().epoch()).isNull();
  }

  @Test
  void percentageCappedAt100() {
    var result = ProgressParser.parse("150% progress (overflow test)");
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo(100);
  }

  @Test
  void epochTakesPrecedenceOverPercentage() {
    var result = ProgressParser.parse("Epoch 2/5 — 75% batch done");
    assertThat(result).isPresent();
    assertThat(result.get().epoch()).isEqualTo(2);
    assertThat(result.get().totalEpoch()).isEqualTo(5);
  }

  @Test
  void noMatchReturnsEmpty() {
    assertThat(ProgressParser.parse("loss: 0.001, accuracy: 0.99")).isEmpty();
    assertThat(ProgressParser.parse("")).isEmpty();
    assertThat(ProgressParser.parse(null)).isEmpty();
    assertThat(ProgressParser.parse("Training started")).isEmpty();
  }

  @Test
  void zeroTotalEpochDoesNotDivide() {
    var result = ProgressParser.parse("Epoch 1/0");
    assertThat(result).isPresent();
    assertThat(result.get().value()).isEqualTo(0);
  }
}
