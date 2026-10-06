# Hướng dẫn Build, Đưa lên Google AI Studio & Chạy trên Android

## 1. Nguyên tắc hoạt động của phần mềm

Phần mềm được thiết kế và tối ưu chuyên biệt theo các nguyên tắc:
1. **Theo dõi tiêu đề lịch công tác:**
   - Quét và nhận diện tự động văn bản lịch làm việc có tiêu đề:
     `LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC CÔNG AN THÀNH PHỐ (DỰ THẢO, điều chỉnh lần 1, 2, 3,....) (Từ ngày 05/10/2026 đến ngày 11/10/2026)`
   - Hỗ trợ đầy đủ các phiên bản: Bản Dự thảo, Lịch gốc chính thức, Điều chỉnh lần 1, lần 2, lần 3, lần 4...
2. **Thu thập và bóc tách dữ liệu chuẩn xác:**
   - Bóc tách tự động thành 6 nhóm trường thông tin:
     - **Nội dung công tác** (`title`)
     - **Thời gian** (`date`, `dayOfWeek`, `time`, `session`: sáng/chiều)
     - **Địa điểm** (`location`)
     - **Chủ trì** (`primaryLeaderId` / tên Lãnh đạo chủ trì)
     - **Người được dự / cùng dự** (`attendees` / `coAttendees`)
     - **Đơn vị chuẩn bị nội dung** (`preparation`)
   - **Nguyên tắc "Dữ liệu nào không có bỏ trống":** Trường nào văn bản trên web không đề cập thì để trống (`""`), tuyệt đối không tự ý suy diễn hoặc thêm thông tin giả định.
   - **Lưu trữ:** Lưu trữ toàn bộ vào cơ sở dữ liệu Room cục bộ (`AppDatabase`).
3. **Đối chiếu và thông báo điều chỉnh:**
   - Khi phát hiện có lịch điều chỉnh mới: Tự động đối chiếu với lịch đã lưu gần nhất.
   - Gửi thông báo hệ thống (Notification) tức thì: Báo rõ số lần điều chỉnh và số nội dung thay đổi/bổ sung.
   - Cung cấp màn hình hiển thị trực quan: Liệt kê chi tiết các nội dung **Bổ sung mới (➕)** và **Đã điều chỉnh (🔄)** so với bản lịch trước đó.
4. **Chu kỳ quét tự động:**
   - Định kỳ đúng **10 phút/lần** truy cập Cổng thông tin: `https://congan.cantho.gov.vn:8888/`.
   - Dịch vụ ngầm `BackgroundSyncService` duy trì quét liên tục ngay cả khi đóng app hoặc tắt màn hình.
5. **Xác thực tự động:**
   - Tên đăng nhập mặc định: `anbd`
   - Mật khẩu: `An@CanTho?2025`
   - Tự động duy trì phiên làm việc và đăng nhập lại khi hết hạn cookie.
6. **Thể hiện rõ trạng thái kết nối:**
   - **Bị chặn truy cập (HTTP 403 / WAF):** Cảnh báo rõ ràng thiết bị đang bị chặn bởi tường lửa hoặc cần kết nối vào mạng nội bộ/VPN ngành CATP.
   - **Không thể truy cập (Mạng / DNS / Timeout / SSL):** Thông báo chi tiết lý do kỹ thuật.
   - **Đã kết nối:** Hiển thị mốc thời gian và trạng thái đồng bộ thành công.

---

## 2. Cách đưa code lên Google AI Studio (aistudio.google.com)

Dự án đã được cấu hình sẵn tệp định danh `metadata.json` theo đúng tiêu chuẩn Google AI Studio:
- Application ID: `com.aistudio.lichbgd.kxmpzq`
- Thư viện Gemini API & Android Compose tích hợp sẵn.

### Các bước đồng bộ lên aistudio.google.com:

1. **Khởi tạo và đẩy mã nguồn lên GitHub:**
   Mở terminal tại thư mục dự án và chạy các lệnh:
   ```powershell
   git init
   git add .
   git commit -m "Hoan thien phan mem Lich Ban Giam Doc CATP theo doi Cong 8888"
   git branch -M main
   git remote add origin https://github.com/<tai-khoan-cua-ban>/<ten-repository>.git
   git push -u origin main
   ```

2. **Import vào Google AI Studio:**
   - Truy cập: **[https://aistudio.google.com](https://aistudio.google.com)** (hoặc [https://ai.studio](https://ai.studio))
   - Đăng nhập bằng tài khoản Google của bạn.
   - Chọn mục **Import App / Get started** ➔ Chọn nguồn **GitHub**.
   - Chọn repository vừa đẩy lên ở bước 1.
   - Google AI Studio sẽ tự động nhận diện dự án Android, liên kết App ID `com.aistudio.lichbgd.kxmpzq` và kích hoạt ứng dụng trên đám mây.

---

## 3. Cách build & chạy ứng dụng trên máy Android

### Cách 1: Chạy trực tiếp từ Android Studio (Khuyến nghị cho nhà phát triển)
1. Mở **Android Studio** (phiên bản Ladybug 2024.2 trở lên).
2. Chọn **Open** ➔ chọn thư mục dự án này.
3. Chờ Android Studio đồng bộ Gradle tự động (phiên bản AGP 9.1.1 + Gradle 9.3.1).
4. Kết nối điện thoại Android thật bằng cáp USB (đã bật **USB Debugging** trong Tùy chọn nhà phát triển) HOẶC tạo một máy ảo Android Emulator.
5. Nhấn nút **Run 'app'** (phím tắt `Shift + F10`) để cài đặt và khởi chạy ứng dụng trực tiếp trên máy.

### Cách 2: Đóng gói file APK cài đặt lên điện thoại Android
1. Trong Android Studio, vào menu:
   **Build ➔ Build Bundle(s) / APK(s) ➔ Build APK(s)**
2. Khi hoàn tất, Android Studio sẽ hiển thị thông báo góc phải dưới:
   Nhấn **locate** để mở thư mục chứa file APK:
   `app/build/outputs/apk/debug/app-debug.apk`
3. Sao chép file `app-debug.apk` này sang điện thoại Android (qua Zalo, Drive, hoặc cáp USB) và nhấn vào file để cài đặt.