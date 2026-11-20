# Composition (v2.0) — Android Camera Pose Overlay, ML Kit Skeleton & Custom Silhouette Guide

Ứng dụng Android (Kotlin + Jetpack Compose + CameraX + Google ML Kit Pose Detection, target Android 15 / API 35) tạo lớp layer ẩn mờ trực tiếp trên nền màn hình camera, tự động gợi ý tư thế chụp theo bố cục khung hình, theo dõi khung xương người mẫu thời gian thực và hỗ trợ nhập ảnh silhouette PNG/JPG tùy chỉnh.

## Tính năng chính

1. **Nhận diện thương hiệu & Logo Composition**
   - Tích hợp đầy đủ bộ biểu tượng ứng dụng thích ứng (*Adaptive Launcher Icons* cho Android 8–15 ở mọi mật độ `mdpi` đến `xxxhdpi`), biểu tượng chữ C ống kính trên thanh điều khiển camera và logo đầy đủ trên màn hình khởi động/xin quyền.

2. **Phân tích bố cục khung hình thời gian thực (`SceneCompositionAnalyzer.kt`)**
   - Phân tích hướng sáng (*Short Lighting* nghiêng trái/phải, Ngược sáng *Rim Light*, Sáng gắt từ trên, Sáng thuận dịu, Thiếu sáng), đường chân trời, hậu cảnh (Phong cảnh mở, Chân trời cao, Tối giản, Hậu cảnh rối) và tiền cảnh (Khung hai bên, Đường dẫn).
   - Tự động chấm điểm và xếp hạng 12 tư thế vector có sẵn, đồng thời tự lật ngang tư thế hướng về nguồn sáng hoặc không gian âm.

3. **Nhận diện khung xương thời gian thực với Google ML Kit (`LivePoseDetector.kt`)**
   - Chạy hoàn toàn ngoại tuyến trên thiết bị (`STREAM_MODE`), vẽ khung xương sống động trực tiếp lên nền preview camera (có nút bật/tắt ngay trên thanh trên cùng).
   - Tự động tính điểm **Khớp dáng (%)** so với tư thế mẫu đang chọn — khung xương và nhãn trạng thái chuyển sang **màu xanh lá** khi đạt từ 80% trở lên để báo hiệu thời điểm bấm chụp lý tưởng.

4. **Nhập ảnh Silhouette PNG / JPG tùy chỉnh (`CustomPoseRepository.kt`)**
   - Nhấn nút **`+ PNG/JPG`** ngay đầu thanh chọn tư thế để chọn bất kỳ ảnh PNG hoặc JPG nào từ máy.
   - Hỗ trợ 2 chế độ nhập:
     - **Tự động tách viền trắng** (bộ lọc biên Sobel): biến ảnh chụp mẫu JPG có nền thành nét viền trắng trên nền trong suốt.
     - **Giữ nguyên ảnh mờ gốc**: giữ nguyên độ trong suốt của ảnh PNG silhouette sẵn có hoặc dùng trực tiếp ảnh tham khảo ở chế độ mờ.
   - Lưu bền vững trong bộ nhớ ứng dụng, hỗ trợ kéo thả, chụm 2 ngón phóng to/thu nhỏ, lật ngang, xóa nhanh và dùng được cả trong **Chế độ lớp phủ nổi trên Camera mặc định** (`FloatingPoseOverlayService.kt`).

## Cách mở & biên dịch trên Android Studio

1. Giải nén tệp `Composition-Android.zip`.
2. Mở **Android Studio** (AGP 8.7.2, JDK 17) -> **Open** -> chọn thư mục `CompositionAndroid`.
3. Chờ Gradle Sync tải CameraX và Google ML Kit Pose Detection, sau đó chạy trên thiết bị Android thật (API 26+).
4. Cấp quyền **Camera** (và quyền **Hiển thị trên ứng dụng khác** nếu dùng chế độ phủ trên ứng dụng Camera mặc định).
