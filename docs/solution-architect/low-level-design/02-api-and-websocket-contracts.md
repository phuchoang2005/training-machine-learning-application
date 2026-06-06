# API and WebSocket Contracts

## Detailed Contract Location

The full modular API contract is maintained in [api-contracts/README.md](./api-contracts/README.md). This file remains a high-level summary for quick navigation.

## REST Conventions

Base path: `/api/v1`

Use JSON for request and response bodies. Use ISO-8601 UTC timestamps. Use UUIDs for entity identifiers. Every protected endpoint must resolve the current authenticated user and enforce ownership or admin rules.

## Authentication APIs

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/auth/me` | Return current user profile and role |
| `POST` | `/auth/logout` | End local session |

Google Workspace login is handled by the Spring Security OIDC flow.

## Project APIs

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/projects` | List projects visible to current user |
| `POST` | `/projects` | Register GitHub project metadata |
| `POST` | `/projects/upload-zip` | Upload and register ZIP project |
| `GET` | `/projects/{projectId}` | Get project detail |
| `DELETE` | `/projects/{projectId}` | Delete project, owner/admin only |

Example project registration:

```json
{
  "projectName": "Fraud Detection",
  "description": "Detect suspicious transactions",
  "sourceType": "GITHUB",
  "repositoryUrl": "https://github.com/company/fraud-model",
  "trainingEntrypoint": "main.py"
}
```

## Configuration APIs

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/projects/{projectId}/configs` | List YAML configs under `./configs/` |
| `GET` | `/projects/{projectId}/configs/{configId}` | Get YAML content |
| `POST` | `/projects/{projectId}/configs/validate` | Validate edited YAML before job start |

## Training Job APIs

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/projects/{projectId}/jobs` | Start a training job |
| `GET` | `/jobs/{jobId}` | Get job detail |
| `GET` | `/projects/{projectId}/jobs` | List project job history |
| `POST` | `/jobs/{jobId}/cancel` | Cancel queued or running job |
| `POST` | `/jobs/{jobId}/retry` | Create a new job from a failed/cancelled job |

Start job request:

```json
{
  "configId": "9f90e7a7-3de1-4d88-b907-2f0fd1f6629f",
  "yamlContent": "artifact_path: outputs/models\ntraining:\n  epochs: 50\n"
}
```

Start job response:

```json
{
  "jobId": "42f7b6d0-b815-4e53-9f20-f393fda4224f",
  "status": "QUEUED",
  "queuePosition": 3
}
```

## Log and Artifact APIs

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/jobs/{jobId}/logs?query=&streamType=&limit=&cursor=` | Search historical logs server-side |
| `GET` | `/jobs/{jobId}/logs/download` | Download combined log file |
| `GET` | `/jobs/{jobId}/artifacts` | List artifacts for a completed job |
| `GET` | `/artifacts/{artifactId}/download` | Download authorized artifact |

## WebSocket Contract

Endpoint: `/ws/jobs/{jobId}`

The backend must authenticate the WebSocket handshake using the same session/JWT as REST APIs. It must authorize the user against the job's project before subscribing.

### Server Events

All server events share this envelope:

```json
{
  "eventId": 1024,
  "jobId": "42f7b6d0-b815-4e53-9f20-f393fda4224f",
  "type": "LOG",
  "timestamp": "2026-06-06T09:15:30Z",
  "payload": {}
}
```

Supported event types:

| Type | Payload |
| --- | --- |
| `JOB_STATUS` | `{ "status": "RUNNING", "queuePosition": null }` |
| `LOG` | `{ "streamType": "STDOUT", "message": "[INFO] epoch 1", "sequenceNo": 10 }` |
| `PROGRESS` | `{ "value": 35, "epoch": 7, "totalEpoch": 20 }` |
| `ARTIFACT_REGISTERED` | `{ "artifactId": "...", "artifactName": "model.pkl" }` |
| `ERROR` | `{ "code": "STREAM_FORBIDDEN", "message": "Not authorized" }` |

### Client Messages

The MVP only requires these client messages:

```json
{ "type": "PING" }
```

```json
{ "type": "RESUME", "lastEventId": 1024 }
```

On reconnect, the client sends `RESUME`; the backend returns missed persisted log/progress/status events when available.
