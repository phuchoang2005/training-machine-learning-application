# Failure Handling Matrix

| Failure Scenario | Detection Point | Platform Action | Job Status | User Notification |
| --- | --- | --- | --- | --- |
| Unauthorized project access | API authorization | Reject request and audit denial | Not created | In-app error |
| GitHub clone failure | Project validation | Stop before execution | FAILED or not created | In-app notification |
| Invalid ZIP package | Project registration | Reject upload | Not created | In-app error |
| Missing `main.py`, `requirements.txt`, or `configs/` | Project validation | Reject registration or job start | FAILED or not created | In-app notification |
| Disk space below 5 GB | Pre-run capacity check | Do not launch container | FAILED | In-app notification |
| Docker container start failure | Runner service | Persist runner error | FAILED | Failure notification |
| Training script exits non-zero | Docker runner | Persist stderr/stdout logs | FAILED | Failure email |
| User cancels queued job | Queue service | Remove queue entry | CANCELLED | In-app status update |
| User cancels running job | Runner service | Stop container gracefully | CANCELLED | In-app status update |
| Server or app restart | Recovery service | Mark interrupted job as RETRYING and requeue | RETRYING then QUEUED | In-app status update |
| Artifact registration failure | Artifact service | Keep training result independent | SUCCESS with artifact error | In-app notification |
| Email delivery failure | Notification service | Persist notification failure | Unchanged | In-app notification |
