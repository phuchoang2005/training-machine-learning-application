# Future – Product Requirements

## Background

Currently, AI model training within the company is performed manually by individual engineers. A typical workflow involves opening a terminal, executing a training command, and waiting for the process to complete. During training, engineers must keep their machines running and continuously monitor the process.

This approach creates several challenges:

* Engineers must keep their local machines occupied during training.
* Training progress is difficult to monitor centrally.
* Failures may go unnoticed until someone manually checks the process.
* There is no centralized history of training runs.
* The workflow does not scale well as the number of AI projects increases.

To address these issues, the company requires Future, a web-based AI training management platform that allows users to launch and monitor training jobs on centralized servers.

---

# Product Vision

Provide a centralized platform where AI engineers can:

* Select an AI project.
* Start model training from a web interface.
* Execute training jobs on dedicated servers.
* Close the browser after launching a training job.
* Monitor training progress and logs remotely.
* Receive notifications when training fails.
* Review historical training runs.

---

# Target Users

## AI Engineers / Data Scientists

Responsibilities:

* Train machine learning and deep learning models.
* Monitor training progress.
* Investigate training failures.
* Manage project-specific training activities.

## ML Platform Engineers / Administrators

Responsibilities:

* Manage training infrastructure.
* Monitor system health.
* Troubleshoot training failures.
* Manage projects, users, and permissions.

---

# Core Functional Requirements

## 1. Authentication

Users must authenticate before accessing the platform.

Requirements:

* Login using company credentials.
* Support a registration entry point for non-production onboarding validation before company SSO/OIDC is enabled.
* Access only authorized projects.
* Support role-based access control.
* Provide clearly labeled non-production sample accounts for local Docker validation.

Sample non-production accounts:

| Role | Email | Password | Notes |
| --- | --- | --- | --- |
| User | `user@example.com` | `password` | Standard AI engineer account seeded for local validation. |
| Admin | `admin@example.com` | `password` | Platform administrator account seeded for local validation. |

The production authentication target remains company SSO/OIDC. Sample credentials must not be used in production.

---

## 2. Project Dashboard

The platform shall display all AI projects accessible to the user.

Each project should display:

* Project name
* Project description
* Latest training status
* Last training time
* Last training owner

Example:

| Project               | Description                    | Latest Status |
| --------------------- | ------------------------------ | ------------- |
| Fraud Detection       | Detect suspicious transactions | Success       |
| Recommendation Engine | Product recommendation model   | Running       |
| Churn Prediction      | Customer churn prediction      | Failed        |

---

## 3. Project Details

When a user selects a project, the platform should display:

* Project information
* Repository location
* Dataset information
* Training configuration
* Training history
* "Start Training" action

---

## 4. Launch Training Job

Users can initiate a training job from the web interface.

Workflow:

1. User clicks "Start Training".
2. Backend creates a new training job.
3. Job is executed on the training server.
4. User may leave the platform or close the browser.
5. Training continues independently on the server.

Training Job Information:

* Job ID
* Project ID
* User ID
* Start time
* End time
* Status
* Log location
* Error message
* Generated model artifacts

Job Statuses:

* PENDING
* RUNNING
* SUCCESS
* FAILED
* CANCELLED

---

## 5. Training Monitoring

Users should be able to monitor training progress through a dedicated Training Detail page.

Displayed information:

* Project name
* Job ID
* Triggered by
* Start time
* Current status
* Running duration
* Training logs

Status updates should be refreshed automatically through polling or WebSocket communication.

---

## 6. Real-Time Log Viewer

The system must provide near real-time access to training logs.

Requirements:

* Stream logs while training is running.
* Automatically append new log entries.
* Support scrolling through logs.
* Support log search/filtering.
* Allow log download after job completion.

Example:

```text
[INFO] Loading dataset...
[INFO] Dataset loaded successfully.
[INFO] Starting epoch 1/50
[INFO] Train loss: 0.5421
[INFO] Validation accuracy: 0.8123
[ERROR] CUDA out of memory
```

---

## 7. Failure Notification via Email

If a training job fails, the platform must automatically notify the user via email.

Email content should include:

* Project name
* Job ID
* Failure timestamp
* Error summary
* Link to training details page
* Relevant error logs

Example subject:

```text
[Future Training Failed] Recommendation Engine - Job #12345
```

---

## 8. Training History

Each project should maintain a complete history of training jobs.

Displayed information:

| Job ID | Triggered By                                | Status  | Start Time | End Time | Duration    |
| ------ | ------------------------------------------- | ------- | ---------- | -------- | ----------- |
| 12345  | [user@company.com](mailto:user@company.com) | Failed  | 10:00      | 10:32    | 32 min      |
| 12344  | [user@company.com](mailto:user@company.com) | Success | 08:00      | 09:10    | 1 hr 10 min |

Users can open any historical job to review details and logs.

---

# User Stories

## User Story 1 – View Projects

As an AI Engineer,

I want to see all projects that I am authorized to access,

so that I can select the correct project for training.

### Acceptance Criteria

* Users see only authorized projects.
* Project list displays latest training status.
* Projects are searchable and accessible.

---

## User Story 2 – Start Training

As an AI Engineer,

I want to launch a training job from the web interface,

so that I do not need to manually run commands in a terminal.

### Acceptance Criteria

* Clicking "Start Training" creates a new training job.
* Training executes on the server.
* Browser closure does not interrupt training.
* Job status becomes visible immediately.

---

## User Story 3 – Monitor Logs

As an AI Engineer,

I want to monitor training logs in real time,

so that I can understand the current execution state.

### Acceptance Criteria

* Logs update continuously.
* Logs remain available after completion.
* Error logs are visible when failures occur.

---

## User Story 4 – Receive Failure Notifications

As an AI Engineer,

I want to receive an email when training fails,

so that I do not need to continuously monitor the platform.

### Acceptance Criteria

* An email is sent whenever a job transitions to FAILED.
* Email includes project information and error details.
* Duplicate notifications are prevented.

---

## User Story 5 – Review Training History

As an AI Engineer,

I want to review historical training jobs,

so that I can investigate previous runs and compare outcomes.

### Acceptance Criteria

* Training history is available per project.
* Users can filter and search historical jobs.
* Users can open previous jobs and inspect logs.

---

# Non-Functional Requirements

## Reliability

* Training jobs must continue running even if users disconnect.
* Job state must be preserved in the event of frontend refreshes.
* System should support recovery from unexpected failures.

## Scalability

* Support multiple concurrent training jobs.
* Allow future integration with GPU clusters.
* Support future Kubernetes-based orchestration.

## Security

* Authentication is required.
* Authorization is enforced per project.
* Training commands must be controlled and validated.
* Sensitive information must never appear in logs.

## Observability

* Platform logs must be available.
* Training logs must be retained.
* Job execution history must be auditable.
* User actions must be traceable.

## Usability

* Simple and intuitive user interface.
* Clear status indicators.
* Easy access to logs and notifications.
* Minimal technical knowledge required for launching jobs.

---

# High-Level Architecture

## Frontend

Responsible for:

* Project management UI
* Training monitoring UI
* Log viewer
* Status dashboards

## Backend API

Responsible for:

* Authentication
* Project management
* Job orchestration
* Notification services

## Training Worker

Responsible for:

* Executing training scripts
* Capturing logs
* Updating job status
* Managing model artifacts

## Database

Stores:

* Users
* Projects
* Training jobs
* Training history
* Notifications

## Storage

Stores:

* Training logs
* Model artifacts
* Checkpoints

## Notification Service

Responsible for:

* Failure notifications
* Future training completion notifications

---

# MVP Success Criteria

The MVP is considered successful if:

1. Users can log into the platform.
2. Users can select an AI project.
3. Users can launch training jobs from the web UI.
4. Training continues independently of the user's browser session.
5. Users can monitor logs during execution.
6. Training history is recorded and accessible.
7. Failure notifications are delivered via email.
