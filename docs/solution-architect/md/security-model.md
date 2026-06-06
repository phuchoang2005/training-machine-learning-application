Mô hình **RBAC + Ownership-Based Access Control** (Phân quyền dựa trên Vai trò kết hợp Quyền sở hữu) là một mô hình kiểm soát truy cập lai (Hybrid Access Control). Nó được tạo ra bằng cách kết hợp hai phương pháp phân quyền phổ biến để giải quyết bài toán quản lý tài nguyên một cách linh hoạt và bảo mật hơn.

Dưới đây là chi tiết về đặc tính, cách hoạt động và thế mạnh lớn nhất của mô hình này.

---

## 1. Thành phần cấu tạo

Để hiểu mô hình lai này, chúng ta phân rã nó thành hai thành phần chính:

- **RBAC (Role-Based Access Control):** Quyền truy cập được gắn liền với **vai trò (Role)** của người dùng trong hệ thống chứ không gắn trực tiếp với cá nhân.
- _Ví dụ:_ Vai trò `Teacher` (Giáo viên) có quyền tạo bài kiểm tra, vai trò `Student` (Học sinh) chỉ có quyền làm bài kiểm tra.

- **Ownership-Based Access Control (Kiểm soát dựa trên quyền sở hữu):** Quyền hạn đối với một tài nguyên cụ thể phụ thuộc vào việc người dùng đó có phải là **chủ sở hữu (Owner/Creator)** của tài nguyên đó hay không.
- _Ví dụ:_ Học sinh A tạo một bài viết trong diễn đàn, Học sinh A có quyền sửa/xóa bài viết đó. Học sinh B (cùng vai trò) không có quyền sửa/xóa bài của Học sinh A.

---

## 2. Đặc tính của mô hình kết hợp (RBAC + Ownership)

Khi kết hợp lại, hệ thống sẽ kiểm tra quyền truy cập qua **hai lớp điều kiện (AND)**:

> **Quyền truy cập = (Vai trò phù hợp) VÀ (Là chủ sở hữu tài nguyên HOẶC có quyền đặc trị)**

### Đặc tính kỹ thuật:

- **Phân quyền theo ngữ cảnh dữ liệu (Data-level/Row-level Security):** RBAC thuần túy chỉ quản lý được ở tầng chức năng (Ví dụ: Trưởng phòng có quyền `Sửa hợp đồng`). Khi kết hợp với Ownership, hệ thống quản lý được sâu xuống tầng dữ liệu (Ví dụ: Trưởng phòng A chỉ có quyền `Sửa hợp đồng do chính Trưởng phòng A tạo ra`).
- **Bảo mật phân cấp và cô lập:** Giúp các người dùng có cùng một vai trò trong hệ thống không thể can thiệp, xem hoặc chỉnh sửa dữ liệu của nhau, đảm bảo tính riêng tư tuyệt đối.
- **Cơ chế ghi đè quyền lực (Admin Override):** Thông thường, mô hình này sẽ giữ lại một số vai trò đặc biệt (như `Admin`, `Super Moderator`) được phép bỏ qua điều kiện Ownership để quản trị hệ thống khi cần thiết.

---

## 3. Thế mạnh lớn nhất của mô hình này (Best Use Cases)

Thế mạnh lớn nhất của mô hình RBAC + Ownership xuất hiện trong các **Hệ thống đa người dùng (Multi-user Platforms) hoặc Hệ thống SaaS (Software-as-a-Service)**, nơi người dùng có vai trò giống nhau nhưng dữ liệu của họ tạo ra phải hoàn toàn độc lập và riêng tư.

Dưới đây là các kịch bản thực tế mà mô hình này phát huy sức mạnh tối đa:

### 1. Hệ thống Quản lý Sự kiện & Bán vé (Event Management & Ticketing)

- **Bối cảnh:** Hệ thống có nhiều nhà tổ chức sự kiện (Event Organizer) cùng đăng ký sử dụng nền tảng.
- **Áp dụng:** Tất cả họ đều có chung vai trò là `Organizer` (đều có quyền `Tạo sự kiện`, `Sửa sự kiện`, `Xem doanh thu`). Tuy nhiên, nhờ có **Ownership**, Organizer A chỉ có thể quản lý, sửa đổi cấu hình vé và xem doanh thu của _chính sự kiện do họ tạo ra_, hoàn toàn không thể can thiệp vào sự kiện của Organizer B.

### 2. Nền tảng Quản lý Nội dung (CMS) và Mạng xã hội

- **Bối cảnh:** Các hệ thống như Medium, WordPress, hoặc các diễn đàn nội bộ.
- **Áp dụng:** Mọi người dùng đăng ký đều có vai trò `Content Creator`. Họ có quyền `Đăng bài viết`. Nhưng nhờ **Ownership**, họ chỉ có quyền `Chỉnh sửa` hoặc `Xóa` các bài viết thuộc quyền sở hữu của mình, trong khi vẫn có quyền `Xem` bài viết của người khác.

### 3. Hệ thống Quản trị Doanh nghiệp (CRM / ERP / HRMS)

- **Bối cảnh:** Đội ngũ nhân viên kinh doanh (Sales) trong một công ty.
- **Áp dụng:** Tất cả nhân viên đều có vai trò `Sales Agent` với quyền `Chăm sóc khách hàng`. Tuy nhiên, để tránh tranh chấp khách hàng, hệ thống áp dụng **Ownership**: Nhân viên nào tự tìm kiếm và nhập thông tin khách hàng lên hệ thống (Owner) thì mới có quyền cập nhật tiến độ hoặc sửa thông tin của khách hàng đó.

---

## Tóm lại

Mô hình **RBAC + Ownership** là giải pháp tối ưu để giải quyết bài toán: **"Làm sao để cấp cùng một bộ tính năng cho nhiều người dùng, nhưng vẫn đảm bảo họ không xâm phạm dữ liệu của nhau?"**. Nó giúp mã nguồn của bạn sạch hơn, không phải tạo ra hàng trăm vai trò động (Dynamic Roles) mà chỉ cần một vài vai trò cố định kết hợp với bộ lọc điều kiện `user_id == resource.owner_id` ở tầng Database hoặc API Gateway.

Bạn có đang dự định thiết kế mô hình này cho một bài toán hay hệ thống cụ thể nào không? Nếu có, hãy chia sẻ để chúng ta cùng tối ưu cấu trúc bảng dữ liệu (Database schema) cho phần này nhé.
