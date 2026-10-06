<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Lịch Ban Giám Đốc Công An Thành Phố - AI Studio Android App

Hệ thống ứng dụng Android theo dõi và tự động đồng bộ lịch làm việc của Ban Giám đốc Công an thành phố Cần Thơ từ Cổng thông tin nội bộ: `https://congan.cantho.gov.vn:8888/`.

## Các chức năng cốt lõi

1. **Giám sát tự động 10 phút/lần:**
   - Dịch vụ nền chạy liên tục định kỳ 10 phút truy cập Cổng `https://congan.cantho.gov.vn:8888/`.
   - Tự động đăng nhập với tài khoản `anbd` và mật khẩu `An@CanTho?2025` khi Cổng yêu cầu.
2. **Theo dõi tiêu đề lịch công tác:**
   - Nhận diện chính xác tiêu đề: `"LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC CÔNG AN THÀNH PHỐ (DỰ THẢO, điều chỉnh lần 1, 2, 3,....) (Từ ngày 05/10/2026 đến ngày 11/10/2026)"`.
3. **Bóc tách dữ liệu trung thực:**
   - Phân tích thành các trường thông tin: Nội dung, Thời gian, Địa điểm, Chủ trì, Người được dự, Đơn vị chuẩn bị nội dung.
   - **Quy tắc:** Dữ liệu nào trên web không có thì bỏ trống, không tự ý suy diễn hoặc thêm nội dung.
4. **Đối chiếu & Báo động điều chỉnh:**
   - Đối chiếu với lịch đã lưu gần nhất để phát hiện nội dung Bổ sung mới (➕) và Đã điều chỉnh (🔄).
   - Bắn thông báo hệ thống (Notification) ngay khi có điều chỉnh.
   - Hiển thị danh sách so sánh chi tiết trực quan.
5. **Hiển thị trạng thái kết nối:**
   - Báo rõ trạng thái khi Cổng chặn truy cập (HTTP 403 / WAF), không thể kết nối (mạng / VPN), hoặc yêu cầu đăng nhập.

## Chạy trên Android & Đồng bộ Google AI Studio

Xem tài liệu chi tiết tại [HUONG_DAN_BUILD.md](HUONG_DAN_BUILD.md):
- **Đưa lên AI Studio:** Đẩy lên GitHub và chọn **Import App** trên [aistudio.google.com](https://aistudio.google.com).
- **Chạy trên máy Android:** Mở bằng Android Studio và nhấn **Run** hoặc xuất file APK cài đặt trực tiếp.
