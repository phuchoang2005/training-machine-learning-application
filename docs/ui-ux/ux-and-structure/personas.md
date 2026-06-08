# User Personas

## 1. Overview

The platform supports operational users who need reliable training execution, monitoring, and governance. Personas are grouped by responsibility and permission level.

## 2. Primary Persona: AI Engineer / Data Scientist

| Attribute | Description |
| --- | --- |
| Role | Builds and trains machine learning or deep learning models. |
| Main Goal | Launch a training job and monitor it without keeping a local machine occupied. |
| Permission Scope | Authorized projects only. Can view project details, start jobs, cancel own or permitted jobs, retry failed jobs, view logs, and download artifacts. |
| Frequency | Daily to weekly depending on project stage. |
| Technical Comfort | High with model training and logs; expects precise status and error messages. |

### Needs

* Find the correct project quickly.
* Confirm branch, dataset version, and configuration before launch.
* See queue position, current status, progress, and logs.
* Search and download logs after completion.
* Access generated artifacts and compare historical runs.
* Receive failure notifications with direct links to job details.

### Pain Points

* Manual terminal sessions require constant attention.
* Training failures are easy to miss.
* Logs and artifacts are scattered across machines.
* Historical runs are difficult to compare.
* A browser refresh should not break monitoring.

### UX Implications

* Put project search, latest status, and last owner on the dashboard.
* Keep `Start Training` close to configuration context.
* Show job status, queue position, and owner in the job header.
* Provide a persistent log viewer with search, auto-scroll, pause, and download.
* Preserve navigation back to project details and history.

## 3. Secondary Persona: Project Owner

| Attribute | Description |
| --- | --- |
| Role | Owns project outcomes, access, and training artifact governance. |
| Main Goal | Review training activity, outputs, and access boundaries for a project. |
| Permission Scope | Own project resources. May manage project membership depending on RBAC policy. |
| Frequency | Weekly or around release checkpoints. |
| Technical Comfort | Medium to high; needs summaries and traceability more than raw log volume. |

### Needs

* Understand the latest training result.
* Review who launched each training job.
* Inspect generated artifacts and failure summaries.
* Confirm that only authorized users can access project resources.
* Track job history for audit and reproducibility.

### Pain Points

* Raw logs can hide the actual failure reason.
* Artifact versioning is hard to audit manually.
* Cross-project access mistakes create governance risk.

### UX Implications

* Include project summary, latest job, and history in a single project detail area.
* Surface owner, start time, end time, duration, and status in history tables.
* Show artifact metadata before download.
* Keep permission errors explicit with a `Forbidden` page or inline blocked-action state.

## 4. Secondary Persona: Platform Administrator

| Attribute | Description |
| --- | --- |
| Role | Maintains users, queue health, training infrastructure, and platform reliability. |
| Main Goal | Keep the training system available and diagnose operational issues. |
| Permission Scope | Admin console, user management, queue visibility, system-wide status, and permitted troubleshooting views. |
| Frequency | Daily monitoring and incident response. |
| Technical Comfort | High with infrastructure, queues, logs, and access control. |

### Needs

* View queue state and current training server activity.
* Diagnose failed, stuck, or retrying jobs.
* Manage users and role assignments.
* Understand audit trails for sensitive actions.
* Cancel jobs when platform stability requires intervention.

### Pain Points

* Queue state can be difficult to explain to users.
* Job ownership and project boundaries must be preserved during support.
* Admin actions require auditability.

### UX Implications

* Separate admin navigation from project workflow navigation.
* Use dense tables for queue and user management.
* Require confirmation for cancellation and role changes.
* Display audit metadata for administrative actions.

## 5. Permission Summary

| Capability | AI Engineer | Project Owner | Platform Administrator |
| --- | --- | --- | --- |
| View assigned projects | Yes | Yes | Yes |
| View all projects | No | No | Depending on policy |
| Start training | Yes, authorized projects | Yes, owned projects | Depending on policy |
| Cancel training | Own or permitted jobs | Project jobs | Platform jobs |
| Retry training | Authorized jobs | Project jobs | Platform jobs |
| View logs | Authorized jobs | Project jobs | Troubleshooting scope |
| Download artifacts | Authorized project artifacts | Project artifacts | Troubleshooting scope |
| Manage users | No | No or limited project membership | Yes |
| View queue monitor | No or limited | Optional read-only | Yes |

