Dựa trên tài liệu hiện có , để viết **High Level Design**, nên hỏi thêm theo bảng này:

| Nhóm cần làm rõ                | Câu hỏi cần trả lời thêm                                                                                                                                   | Có thể vẽ/tạo ra được gì                                  |
| ------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------- |
| **1. System Context**          | Ai dùng hệ thống? User, Admin, Google Workspace, GitHub, Docker Engine tương tác ra sao?                                                                   | **System Context Diagram**                                |
| **2. Component Architecture**  | Hệ thống nên tách thành những module nào: Project Service, Job Service, Queue Service, Docker Runner, Log Service, Artifact Service? | **High-Level Component Diagram**                          |
| **3. Deployment**              | Toàn bộ chạy trên 1 server hay nhiều server? DB, storage, Docker host, backend, frontend đặt ở đâu?                                                        | **Deployment Diagram**                                    |
| **4. Training Job Flow**       | Từ lúc user bấm Train đến khi container chạy, stream log, lưu artifact diễn ra thế nào?                                                         | **Training Execution Sequence Diagram**                   |
| **5. Queue & Scheduling**      | FIFO queue lưu ở DB hay message queue? Khi có 2 job đang chạy thì job thứ 3 được xử lý ra sao?                                                             | **Queue Flow Diagram / Job State Machine**                |
| **6. Job State**               | Job có những trạng thái nào: CREATED, QUEUED, RUNNING, SUCCESS, FAILED, CANCELLED, RETRYING? Chuyển trạng thái theo điều kiện nào?                         | **Job Lifecycle State Diagram**                           |
| **7. Restart Recovery**        | Khi server restart, job RUNNING cũ được phát hiện và requeue bằng cơ chế nào?                                                                              | **Recovery Flow Diagram**                                 |
| **8. Log Streaming**           | Log từ stdout/stderr được đọc bằng process nào? Đẩy lên UI bằng WebSocket; polling chỉ dùng làm fallback? Lưu log ở file hay DB?                           | **Log Streaming Architecture Diagram**                    |
| **9. Progress Tracking**       | Python training code emit progress qua stdout JSON, file, HTTP callback hay socket? Platform parse thế nào?                                                | **Progress Event Flow Diagram**                           |
| **10. Artifact Management**    | Artifact nằm trong container path hay mounted volume? Sau job SUCCESS, hệ thống scan artifact_path và register thế nào?                                    | **Artifact Flow Diagram**                                 |
| **11. Git/ZIP Project Intake** | Project từ GitHub clone và ZIP upload đi qua các bước validate nào? Lưu source code ở đâu?                                                                 | **Project Registration Flow Diagram**                     |
| **12. Config Snapshot**        | YAML được edit ở UI như raw text hay editor chuyên dụng? Snapshot lưu trong DB hay file?                                                                   | **Configuration Management Flow**                         |
| **13. Security Boundary**      | User chỉ thấy project của mình bằng cơ chế nào? Admin bị chặn source/artifact/log chi tiết ra sao?                                                         | **Access Control Matrix / Security Architecture Diagram** |
| **14. Data Model**             | Cần những bảng nào: users, projects, jobs, job_logs, artifacts, model_versions, config_snapshots, audit_logs?                               | **ERD / Logical Data Model**                              |
| **15. API Design**             | Frontend gọi những API nào để tạo project, train, cancel, retry, xem log, download artifact?                                                               | **API List / API Interaction Diagram**                    |
| **16. Storage Design**         | Source code, uploaded ZIP, extracted project, logs, artifacts, model versions lưu ở đâu? Có folder convention không?                                       | **Storage Layout Diagram**                                |
| **17. Failure Handling**       | Git clone fail, disk < 5GB, container crash, email fail, artifact registration fail được xử lý thế nào?                                                    | **Failure Handling Matrix / Error Flow Diagram**          |
| **18. Capacity & Scalability** | Vì chỉ 7 active users và 2 running jobs, cần limit ở tầng nào? Có cần scale sau MVP không?                                                                 | **Capacity View / Scalability Roadmap**                   |

Các sơ đồ quan trọng nhất nên có trong HLD:

1. **System Context Diagram**
2. **High-Level Component Diagram**
3. **Deployment Diagram**
4. **Training Job Sequence Diagram**
5. **Job Lifecycle State Diagram**
6. **Queue & Recovery Flow Diagram**
7. **Log/Progress Streaming Diagram**
8. **Artifact & Model Versioning Flow**
9. **ERD mức logical**
10. **Security & Permission Matrix**
