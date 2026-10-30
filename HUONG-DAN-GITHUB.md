# Build APK không cần Android Studio (dùng GitHub Actions)

1. Vào github.com, tạo repository mới (để trống, không tick README).
2. Giải nén file ZIP này, tải toàn bộ nội dung lên repository (nút "uploading an existing file"
   hoặc kéo thả): thư mục `android/`, thư mục `.github/` và file này phải nằm ở gốc repo.
3. Vào tab Actions của repo, chọn workflow "Build APK", chờ chạy xong (lần đầu vài phút).
4. Kéo xuống cuối trang chạy workflow, mục Artifacts, tải `app-debug-apk` về, giải nén,
   chép file `app-debug.apk` sang điện thoại và cài đặt.
5. Mở app, vào Settings, điền địa chỉ backend (IP máy chạy backend) rồi mới dùng tính năng dịch.

Lưu ý: app này cần backend mới dịch được thật. Chưa chạy backend thì app sẽ báo lỗi rõ ràng,
không giả vờ dịch.
