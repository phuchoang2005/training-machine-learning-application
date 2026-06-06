| Chức năng / Tài nguyên |        User |         Admin | Ghi chú bảo mật                   |
| ---------------------- | ----------: | ------------: | --------------------------------- |
| Đăng nhập / đăng xuất  |          ✅ |            ✅ | Ghi audit log                     |
| Xem dashboard cá nhân  |          ✅ |            ✅ | User chỉ thấy dữ liệu của mình    |
| Tạo project            |          ✅ |            ❌ | Admin không tạo project thay user |
| Xem danh sách project  |    Own only | Metadata only | Admin chỉ xem tên project + owner |
| Xem source code        |    Own only |            ❌ | Admin bị chặn                     |
| Upload ZIP project     |          ✅ |            ❌ | Validate file type + size         |
| Cấu hình YAML          |    Own only |            ❌ | Snapshot immutable khi job start  |
| Start training job     |    Own only |            ❌ | Kiểm tra quota + disk ≥ 5GB       |
| Cancel running job     |     Own job |       Any job | Admin được cancel job đang chạy   |
| Retry job              |    Own only |            ❌ | Retry tạo job mới                 |
| Xem log chi tiết       |    Own only |            ❌ | Admin không inspect business data |
| Stream progress/log    |    Own only |            ❌ | WebSocket phải kiểm tra ownership |
| Download artifact      |    Own only |            ❌ | Admin bị chặn download            |
| Delete project         | Own project |   Any project | Ghi audit log bắt buộc            |
| Xem notification       |    Own only |            ❌ | Notification theo user            |
| Xem audit log          | Own actions |  System-level | Admin không xem nội dung nhạy cảm |
| Quản lý user           |          ❌ |            ✅ | Tối thiểu cho MVP                 |
