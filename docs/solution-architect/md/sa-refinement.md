# AI Training Management Platform

## Architecture Clarification and Confirmed Solution Assumptions

### Document Status

Approved for Solution Architecture Design Phase

### Purpose

This document records all architecture-related clarifications agreed between Business Analysis and Solution Architecture teams. The purpose of this document is to eliminate ambiguity before proceeding with High-Level Design (HLD), Low-Level Design (LLD), implementation planning, and effort estimation.

---

# 1. Training Execution Architecture

## 1.1 Execution Model

Each training job shall be executed inside an isolated Docker container.

The platform shall not execute Python training scripts directly on the host operating system.

---

## 1.2 Training Entry Point

Every AI project must explicitly provide a training entry point.

Example:

```text
main.py
```

The platform shall invoke the configured training entry point when launching a training job.

---

## 1.3 Environment Isolation

Environment isolation between projects is mandatory.

Each project may have different Python dependencies and runtime requirements.

The platform shall build or run an isolated Docker environment for each training job.

---

## 1.4 Dependency Management

Every project must provide:

```text
requirements.txt
```

The platform shall install dependencies from this file during the build or execution process.

---

# 2. Source Code Management

## 2.1 Supported Repository Provider

For MVP, source code repositories shall be hosted on GitHub.

Only public repositories are supported.

Authentication tokens are not required.

---

## 2.2 Repository Retrieval Strategy

When a training job is executed:

* If the repository does not exist locally, the platform shall clone the repository.
* Existing local repositories shall not be updated using Git Pull.
* Every training execution shall use the originally cloned repository content.

---

## 2.3 ZIP Upload Support

In addition to GitHub repositories, users may upload a ZIP package containing a project.

The platform shall:

1. Accept ZIP uploads.
2. Extract the package automatically.
3. Detect project structure.
4. Validate required project files.
5. Register the project for training.

---

# 3. Dataset Management

Dataset version management is intentionally excluded from platform scope.

Dataset download, version selection, validation, and lifecycle management shall be handled entirely by the project’s Python code.

The platform shall not provide dataset version selection functionality.

---

# 4. Training Configuration Management

## 4.1 Configuration Location

Training configurations shall be stored under:

```text
./configs/
```

within the project.

---

## 4.2 YAML Editing

Users shall be allowed to edit the entire YAML configuration file.

No field-level restrictions are applied.

---

## 4.3 Nested Configuration Support

The platform shall support editing nested YAML structures.

Example:

```yaml
optimizer:
  adam:
    lr: 0.001
```

---

## 4.4 Configuration Snapshot

A complete configuration snapshot shall be preserved for every training execution.

The snapshot shall be immutable once the training job has started.

---

# 5. Job Scheduling and Queue Management

## 5.1 Concurrent Execution Capacity

The platform shall support:

```text
2 concurrent RUNNING jobs
```

at any given time.

---

## 5.2 Queue Policy

Additional jobs shall be placed into a FIFO queue.

Queued jobs shall automatically start when execution capacity becomes available.

---

## 5.3 Queue Persistence

Queued jobs must survive:

* Platform restart
* Server restart
* Application redeployment

No queued job shall be lost due to restart events.

---

# 6. Running Job Recovery

## 6.1 Recovery Policy

When the server or application is restarted:

Running jobs shall not be marked as FAILED.

Instead, the platform shall automatically requeue and rerun interrupted jobs.

---

## 6.2 Restart Behavior

The system shall prioritize execution recovery and business continuity over preserving partial execution state.

Restarted jobs may begin execution from the beginning.

Checkpoint-based recovery is not required for MVP.

---

# 7. Logging Architecture

## 7.1 Log Sources

Training logs may originate from:

* stdout
* stderr

Both streams shall be supported.

---

## 7.2 Live Log Streaming

The platform shall provide real-time log streaming during execution.

---

## 7.3 Historical Logs

Logs shall remain available until explicitly deleted by the user.

No automatic retention period is defined.

---

## 7.4 Log Organization

Each training execution shall create an independent log tab or log session.

Logs from different jobs shall not be merged.

---

## 7.5 Search Capability

Log search shall be performed server-side.

Client-side searching is not permitted for MVP.

---

# 8. Artifact Management

## 8.1 Artifact Path Configuration

Artifact storage locations shall be defined inside project YAML configurations.

Example:

```yaml
artifact_path: outputs/models
```

The platform shall use this configuration to discover generated artifacts.

---

## 8.2 Artifact Registration

Artifacts generated by successful training jobs shall be registered automatically by the platform.

---

# 9. Model Identification and Versioning

## 9.1 Project-Level Model Ownership

Every model shall be associated with:

* Project
* Training Job

---

## 9.2 Model Naming

The platform shall preserve:

* Project Name
* Model Name

to uniquely identify generated models.

---

## 9.3 Version Tracking

Model versions shall be tracked per project.

Version history must remain linked to the originating training job.

---

# 10. Retry Behavior

Retry operations shall always create a new training job.

The platform shall generate:

* New Job ID
* New Execution Record

The retry relationship shall remain traceable to the original job.

---

# 11. Notification Architecture

## 11.1 Email Provider

Email notifications shall be sent through Google Workspace infrastructure.

---

## 11.2 Email Failure Handling

Failure to send email notifications shall not affect training job status.

Example:

```text
Training Job = SUCCESS
Email Delivery = FAILED
```

In such cases, the platform shall display a notification to the user.

---

# 12. Security and Access Control

## 12.1 User Ownership Model

The platform is designed primarily for individual users rather than collaborative teams.

---

## 12.2 User Permissions

Users shall have full control over their own projects.

---

## 12.3 Administrator Permissions

Administrators may:

* View project names
* View project ownership
* Cancel running jobs
* Delete projects

Administrators shall not:

* Access project source code
* Download artifacts
* View detailed project content
* Inspect business data

---

# 13. Audit Logging

All audit records shall be retained indefinitely.

Audit records remain available until explicitly deleted.

Examples include:

* User login
* Training start
* Training cancellation
* Training retry
* Artifact download
* Project deletion

---

# 14. Capacity Constraints

## 14.1 Training Limits

No restrictions are imposed on:

* Model size
* Training duration
* Number of jobs per day

Training jobs may continue until completion.

---

## 14.2 Concurrent Platform Users

The platform shall support a maximum of:

```text
7 concurrent active users
```

When capacity is exceeded, new users shall receive a platform busy notification.

---

# 15. Failure Handling Policies

## 15.1 Git Clone Failure

Repository cloning failures shall generate platform notifications.

Training execution shall not start.

---

## 15.2 Dataset Download Failure

Dataset download is the responsibility of project code.

Any dataset-related failures shall be recorded through the training log stream.

---

## 15.3 Server Restart

Interrupted running jobs shall be automatically retried.

---

## 15.4 Disk Capacity Validation

Before launching a training job, the platform shall verify:

```text
Minimum available disk space: 5 GB
```

Jobs shall not start if this requirement is not satisfied.

---

## 15.5 Artifact Registration Failure

If artifact registration or upload fails, the platform shall notify the user through the platform notification system.

Training status shall remain independent from artifact registration status whenever possible.

---

# 16. Approval for Architecture Design

The requirements and assumptions documented above are considered approved inputs for the Solution Architecture phase.

Solution Architecture may proceed with:

* High-Level Design (HLD)
* Low-Level Design (LLD)
* Database Design
* Infrastructure Design
* API Design
* Implementation Estimation

Any future change to these assumptions shall be processed through formal requirement change management.

# 15A. Training Progress Tracking

## 15A.1 Progress Visibility

The platform shall display training progress information to users in real time.

The objective is to allow users to understand the current execution state without relying solely on log inspection.

---

## 15A.2 Progress Source

Training progress information shall be provided by the project’s Python training code.

The platform shall not calculate training progress independently.

The training application is responsible for reporting progress updates.

---

## 15A.3 Progress Percentage

The platform shall support displaying a training completion percentage.

Example:

```text
0%
15%
42%
78%
100%
```

The percentage value shall be reported by the training code and transmitted to the platform during execution.

---

## 15A.4 User Interface Requirements

The Training Detail page shall display:

* Progress Percentage
* Progress Bar
* Current Status
* Running Duration

Example:

```text
Training Progress

[██████████░░░░░░░░░░] 50%

Status: RUNNING
Elapsed Time: 01:25:34
```

---

## 15A.5 Update Mechanism

Progress updates shall use the same real-time communication mechanism used for monitoring:

* WebSocket as the primary mechanism
* REST polling as a fallback when WebSocket is unavailable

---

## 15A.6 Progress Accuracy

The platform shall display the latest progress value received from the training process.

The platform is not responsible for validating or recalculating progress percentages.

Accuracy of the progress value is the responsibility of the project’s training code.

---

## 15A.7 Failure Handling

If no progress information is provided by the training code:

* The training job shall continue normally.
* The platform shall display:

```text
Progress Information Not Available
```

instead of a percentage indicator.

---

## 15A.8 Integration Contract

For consistency across projects, training applications should emit structured progress events.

Example:
``` json
{
  "event":"progress",
  "value":35,
  "epoch":7,
  "total_epoch":20
}
```

The platform shall parse these events and update the Training Detail page accordingly.
