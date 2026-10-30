# AI VIDEO TRANSLATOR PRO — Android App

Thương hiệu riêng, giao diện tiếng Việt mặc định, nền xanh đen/navy + xanh
neon + trắng/xám bạc, Dark Mode. Không dùng logo hay tài sản của HeyGen
hay bất kỳ ứng dụng nào khác.

## Mở bằng Android Studio

1. Mở thư mục `android/` (thư mục chứa `settings.gradle.kts`) bằng Android Studio.
2. Android Studio sẽ tải Gradle 8.5 theo
   `gradle/wrapper/gradle-wrapper.properties` và đồng bộ project.
   Nếu thiếu file `gradlew`, chạy `gradle wrapper` một lần từ máy đã có
   Gradle, hoặc dùng nút Run trong Android Studio để tạo wrapper.
3. Chạy trên emulator hoặc điện thoại Android 10+ (minSdk 29,
   targetSdk 34, compileSdk 34, package `com.avtpro.translator`).

> Lưu ý môi trường: bản này được viết và kiểm tra khớp tên class/route/
> import bằng tay, **chưa chạy `./gradlew assembleDebug`** vì máy viết code
> không có Android SDK/Java. Trước khi phát hành, bắt buộc chạy build thật
> trên máy có SDK và sửa lỗi (nếu có) do phiên bản thư viện thực tế.

## Cấu trúc

```
settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.properties   (Gradle 8.5)
app/build.gradle.kts                       (Kotlin + Compose + Material 3)
app/src/main/AndroidManifest.xml           (INTERNET, READ_MEDIA_VIDEO,
                                            READ_EXTERNAL_STORAGE<=32,
                                            POST_NOTIFICATIONS)
app/src/main/java/com/avtpro/translator/
  MainActivity.kt
  navigation/Routes.kt, AppNavHost.kt
  ui/theme/Color.kt, Theme.kt, Type.kt
  ui/components/Common.kt
  ui/screens/HomeScreen.kt, VideoTranslationScreen.kt,
             AudioDubbingScreen.kt, SubtitleStudioScreen.kt,
             ProcessingScreen.kt, ResultScreen.kt,
             HistoryScreen.kt, SettingsScreen.kt
  data/model/Models.kt
  data/api/ApiService.kt, RetrofitClient.kt
  data/settings/SettingsStore.kt           (DataStore Preferences)
  data/history/ProjectRepository.kt        (JSON file projects.json)
  util/VideoMetadata.kt, FileUtils.kt, SubtitleUtils.kt
  viewmodel/TranslationViewModel.kt, SettingsViewModel.kt,
            HistoryViewModel.kt
app/src/main/res/values/strings.xml, themes.xml
app/src/main/res/xml/file_paths.xml        (FileProvider cho Share)
```

## Navigation (route phải khớp đúng tên này)

| Route | Màn hình |
|---|---|
| `home` | Home: logo, Chọn video, Video Translation, Audio Dubbing, Auto Subtitle, Voice Studio (-> Audio Dubbing), Video History, Settings |
| `translation` | VideoTranslation: VideoView preview, metadata thật (MediaMetadataRetriever), Auto Detect, ngôn ngữ đích có tìm kiếm, Voice Mode, 4 toggle, START AUTO TRANSLATION |
| `audio_dubbing` | AudioDubbing: chọn audio, nhận diện, dịch, chọn giọng, xuất MP3/WAV |
| `subtitle_studio?projectId={projectId}` | SubtitleStudio: sửa text/thời gian, font/màu/vị trí, xuất SRT |
| `processing/{jobId}/{projectId}` | Processing: 9 bước + % thật từ server, dừng polling |
| `result/{projectId}` | Result: xem video, Play, Download, Share, Export Subtitle, Translate Again, Delete Project |
| `history` | History: danh sách project local |
| `settings` | Settings: ngôn ngữ, giọng, Online/Offline/Auto, chất lượng, backend URL, model offline, riêng tư |

## Luồng thật (không tiến độ giả)

```
Chọn video (GetContent video/*)
  -> VideoTranslation: metadata thật, không ước lượng
  -> START -> dialog xác nhận quyền nếu chọn Voice Similarity
  -> POST /api/video/upload (multipart, field "file")
  -> POST /api/video/translate (xem Models.TranslateRequest)
  -> poll GET /api/jobs/{id} mỗi 2 giây
  -> hiển thị current_step + progress đúng như server trả
  -> khi status=completed: GET /api/video/result/{id}, tải file về
  -> lưu Project vào projects.json, mở Result
```

- Không tự tăng %, không báo 100% khi server chưa `completed`.
- Dừng polling chỉ hủy coroutine theo dõi trên app; không tự nhận đã hủy
  job trên server (trừ khi backend hỗ trợ endpoint hủy riêng).
- Offline: nếu không tới được backend, app báo rõ
  "chưa có model offline trong bản này, cần kết nối backend" (chế độ
  Offline) hoặc tương tự ở chế độ Auto. Không giả lập xử lý offline.
- Không nhúng API key nào. Backend URL cấu hình trong Settings, mặc định
  `http://10.0.2.2:8000` cho emulator; điện thoại thật đổi sang IP LAN
  của máy chạy backend, ví dụ `http://192.168.1.10:8000`.

## Hợp đồng backend mà app đang giả định (bắt buộc khớp)

### POST /api/video/upload (multipart, field `file`)
Trả về: `{ "project_id": "...", "filename": "..." }`

### POST /api/video/translate (JSON)
```json
{
  "project_id": "...", "target_language": "vi",
  "voice_mode": "natural | similarity",
  "voice_gender": "male | female",
  "subtitle_on": true, "bilingual": false,
  "hardcode_subtitle": true,
  "keep_background": false, "keep_original_audio": false,
  "voice_similarity_consent": false,
  "translation_style": "natural"
}
```
- Backend tự nhận diện ngôn ngữ gốc, app không gửi `source_language`.
- `voice_mode="similarity"` chỉ được gửi kèm `voice_similarity_consent=true`
  khi user đã bấm đồng ý trong dialog; không đồng ý thì app gửi `natural`.
- `keep_background=false` là mặc định: `true` yêu cầu demucs, nếu không có
  demucs thì job sẽ FAILED rõ ràng thay vì lặng lẽ mất nhạc nền.
Trả về: `{ "job_id": "..." }`

### GET /api/jobs/{id}
```json
{
  "id": "...", "status": "queued|processing|completed|failed|cancelled",
  "current_step": "Translate Text",
  "progress": 45,
  "error": null,
  "source_language": "zh", "target_language": "vi"
}
```
- Backend trả `id` (không phải `job_id`); app map qua `@SerializedName`
  alternate. `progress` là 0..100 do server tính.
- `progress` là 0..100 do server tính. `step_index` (0..8) khớp 9 bước trong
  `PROCESSING_STEPS`; nếu thiếu, app tự map từ `current_step` theo từ khóa,
  nếu vẫn không map được thì chỉ hiển thị `current_step` dạng text.

### GET /api/video/result/{id}
```json
{
  "id": "...",
  "result_download_url": "/api/files/<id>/video",
  "subtitle_srt_url": "/api/files/<id>/srt",
  "source_language": "zh", "target_language": "vi"
}
```
- App map `result_download_url` -> `videoUrl` và `subtitle_srt_url` ->
  `subtitleUrl` qua `@SerializedName` alternate, rồi tải bằng `downloadFile`.
- Nếu backend chỉ trả URL dạng đường dẫn tương đối, app tự nối với
  backend URL đang cấu hình.

### POST /api/audio/transcribe (multipart `file`)
Trả về: `{ "language": "zh", "segments": [...] }` (mỗi segment có
`start`, `end`, `text`).

### POST /api/audio/dub (multipart)
Các part: `file`, `target_language`, `voice_mode` (`natural`/`similarity`),
`voice_similarity_consent`, `voice_gender`, `output_format` (`mp3`/`wav`).
Trả về: `{ "dub_id": "...", "download_url": "/api/dubs/<id>/file", ... }`
và app map `download_url` -> `audioUrl`.

### POST /api/subtitles/generate, DELETE /api/projects/{id}
Đã định nghĩa trong `ApiService`. Màn hình SubtitleStudio hiện tại xuất SRT
local và lưu lại vào project, chưa gọi `/api/subtitles/generate` (xem mục
"Chưa làm").

## Những điểm app còn phụ thuộc backend (chưa chạy được nếu thiếu)

1. **Toàn bộ AI**: ASR (Whisper/Faster-Whisper...), dịch, TTS, tách giọng/
   tách nhạc, căn thời gian, render FFmpeg — đều nằm ở backend. Nếu backend
   chưa có model nào, job sẽ `failed` với `error` do backend trả, app chỉ
   hiển thị lỗi đó, không tự tạo video.
2. **Upload/translate/job/result**: nếu backend chưa implement đúng tên
   field/route trên, app sẽ lỗi HTTP và hiển thị lỗi kết nối.
3. **Voice Similarity**: backend phải tự kiểm tra quyền/đồng ý phía server;
   dialog trên app chỉ là cổng phía client.
4. **Xóa khỏi server**: History trong bản này chỉ xóa file local +
   record JSON. `DELETE /api/projects/{id}` đã có trong `ApiService`
   nhưng `HistoryViewModel` chưa gọi vì nó không giữ backend URL; cần nối
   SettingsStore vào HistoryViewModel khi backend sẵn sàng.
5. **Offline thật**: chưa có model offline nào đóng gói. Mục "Quản lý model
   offline" trong Settings chỉ hiển thị trạng thái, không tải model giả.
6. **Dịch lại ngôn ngữ khác**: app không giữ file gốc đã upload, chỉ giữ
   video kết quả. Muốn dịch lại, user chọn lại video gốc ở Home rồi chọn
   ngôn ngữ khác.
7. **Bản dịch giao diện**: Settings lưu `app_language` nhưng giao diện
   bản này vẫn hiển thị tiếng Việt; cần thêm string resources cho ngôn ngữ
   khác mới đổi thật.
8. **APK**: chưa build được ở máy viết code (không có SDK/Java). Cần chạy
   `./gradlew assembleDebug` trên máy có Android SDK để có APK thật và
   kiểm thử quy trình nghiệm thu bằng video tiếng Trung thật.

## Kiểm thử nghiệm thu (khi backend + SDK sẵn sàng)

1. Mở app, chọn video tiếng Trung thật.
2. Chọn đích tiếng Việt, chế độ Auto, nhấn START.
3. Kiểm tra: server nhận diện `zh`, dịch sang `vi`, tạo giọng Việt, tạo
   phụ đề Việt, đồng bộ, ghép MP4.
4. Mở Result, Play, Download, Share, Export Subtitle, Delete Project.
5. Tắt backend rồi thử lại: app phải báo rõ chưa có model offline, không
   chạy tiến độ giả.
