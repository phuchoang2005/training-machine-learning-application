package com.example.aitraining.runner;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless utility that parses a single training-output line for progress signals
 * (NFR-TEST-001, NFR-UX-002).
 *
 * <p>Recognised patterns, in precedence order:
 * <ol>
 *   <li>{@code Epoch N/M} (case-insensitive) — yields epoch, total, and a percentage.</li>
 *   <li>{@code Step N/M} (case-insensitive) — yields only a percentage.</li>
 *   <li>{@code NN%} — yields only a percentage.</li>
 * </ol>
 *
 * <p>When none of the patterns match, {@link #parse} returns {@link Optional#empty()} and the
 * caller emits no progress event, leaving the UI to display the "Progress Information Not
 * Available" state (NFR-UX-002).
 */
public final class ProgressParser {

  private static final Pattern EPOCH = Pattern.compile("(?i)epoch\\s+(\\d+)\\s*/\\s*(\\d+)");
  private static final Pattern STEP  = Pattern.compile("(?i)step\\s+(\\d+)\\s*/\\s*(\\d+)");
  private static final Pattern PERCENT = Pattern.compile("\\b(\\d{1,3})%");

  private ProgressParser() {}

  /**
   * Parsed progress snapshot extracted from a single log line.
   *
   * @param value      integer percentage in [0, 100]
   * @param epoch      current epoch number, or {@code null} when not available
   * @param totalEpoch total epoch count, or {@code null} when not available
   */
  public record Progress(int value, Integer epoch, Integer totalEpoch) {}

  /**
   * Parses a log line for a progress signal.
   *
   * @param line a single line of training-process output; may be {@code null} or blank
   * @return a {@link Progress} if a recognised pattern was found, otherwise empty
   */
  public static Optional<Progress> parse(String line) {
    if (line == null || line.isBlank()) {
      return Optional.empty();
    }

    Matcher epochMatcher = EPOCH.matcher(line);
    if (epochMatcher.find()) {
      int e     = Integer.parseInt(epochMatcher.group(1));
      int total = Integer.parseInt(epochMatcher.group(2));
      int pct   = total > 0 ? Math.min(100, e * 100 / total) : 0;
      return Optional.of(new Progress(pct, e, total));
    }

    Matcher stepMatcher = STEP.matcher(line);
    if (stepMatcher.find()) {
      int s     = Integer.parseInt(stepMatcher.group(1));
      int total = Integer.parseInt(stepMatcher.group(2));
      int pct   = total > 0 ? Math.min(100, s * 100 / total) : 0;
      return Optional.of(new Progress(pct, null, null));
    }

    Matcher pctMatcher = PERCENT.matcher(line);
    if (pctMatcher.find()) {
      int value = Math.min(100, Integer.parseInt(pctMatcher.group(1)));
      return Optional.of(new Progress(value, null, null));
    }

    return Optional.empty();
  }
}
