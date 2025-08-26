# Hướng dẫn Setup và Chạy Dự án Android Game

## Yêu cầu hệ thống

### 1. Cài đặt Java Development Kit (JDK)
- **Yêu cầu**: JDK 11 hoặc cao hơn
- **Tải về**: [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html) hoặc [OpenJDK](https://openjdk.org/)
- **Kiểm tra**: Mở Command Prompt/Terminal và chạy `java -version`

### 2. Cài đặt Android Studio
- **Tải về**: [Android Studio](https://developer.android.com/studio)
- **Phiên bản**: Android Studio Flamingo hoặc mới hơn (để hỗ trợ Kotlin 2.0.21)

### 3. Cấu hình Android SDK
- **SDK Platform**: Android API 36 (Android 15)
- **Build Tools**: 34.0.0 hoặc mới hơn
- **Gradle**: 8.12.1 (tự động tải về)

## Hướng dẫn Setup

### Bước 1: Clone/Download dự án
```bash
git clone [repository-url]
cd lap_trinh_game
```

### Bước 2: Mở dự án trong Android Studio
1. Mở Android Studio
2. Chọn "Open an Existing Project"
3. Navigate đến thư mục `game` (không phải thư mục root)
4. Chọn thư mục `game` và click "OK"

### Bước 3: Cấu hình SDK (nếu cần)
1. Vào **File > Project Structure**
2. Trong tab **SDK Location**:
   - Android SDK Location: Đảm bảo đường dẫn SDK đúng
   - JDK Location: Chọn JDK 11 hoặc cao hơn

### Bước 4: Sync Project
1. Android Studio sẽ tự động sync project
2. Nếu không, click **Sync Now** trong notification bar
3. Đợi quá trình download dependencies hoàn thành

### Bước 5: Tạo/Kết nối thiết bị
#### Option A: Sử dụng Android Emulator
1. Vào **Tools > AVD Manager**
2. Click **Create Virtual Device**
3. Chọn device (ví dụ: Pixel 7)
4. Chọn System Image: **API 36 (Android 15)** hoặc **API 34 (Android 14)**
5. Click **Finish** và **Start**

#### Option B: Sử dụng thiết bị thật
1. Bật **Developer Options** trên điện thoại
2. Bật **USB Debugging**
3. Kết nối điện thoại qua USB
4. Cho phép debugging khi popup xuất hiện

## Chạy dự án

### Cách 1: Sử dụng Android Studio
1. Đảm bảo device/emulator đã kết nối
2. Click nút **Run** (tam giác xanh) hoặc nhấn **Shift + F10**
3. Chọn target device
4. Đợi build và install hoàn thành

### Cách 2: Sử dụng Command Line
```bash
# Mở Terminal trong Android Studio hoặc Command Prompt
cd game

# Build debug APK
./gradlew assembleDebug

# Install và run
./gradlew installDebug
```

## Troubleshooting

### Lỗi thường gặp:

#### 1. "SDK location not found"
**Giải pháp**: 
- Tạo file `local.properties` trong thư mục `game/`
- Thêm dòng: `sdk.dir=C\:\\Users\\[username]\\AppData\\Local\\Android\\Sdk` (Windows)

#### 2. "Gradle sync failed"
**Giải pháp**:
- Kiểm tra kết nối internet
- **File > Invalidate Caches and Restart**
- Xóa thư mục `.gradle` và sync lại

#### 3. "Minimum supported Gradle version is X.X"
**Giải pháp**:
- Cập nhật Android Studio lên phiên bản mới nhất
- Hoặc downgrade Gradle version trong `gradle/wrapper/gradle-wrapper.properties`

#### 4. "Unable to resolve dependency"
**Giải pháp**:
- Kiểm tra kết nối internet
- **File > Sync Project with Gradle Files**

### Performance Tips:
1. **Tăng RAM cho Gradle**: File `gradle.properties` đã được cấu hình với `-Xmx2048m`
2. **Enable Parallel builds**: Uncomment `org.gradle.parallel=true` trong `gradle.properties`
3. **Use SSD**: Chạy dự án trên ổ SSD để tăng tốc độ build

## Thông tin dự án

- **Package Name**: `com.example.mygame`
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 36 (Android 15)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Build System**: Gradle with Kotlin DSL

## Cấu trúc thư mục quan trọng

```
game/
├── app/src/main/
│   ├── java/              # Kotlin source code
│   ├── res/               # Resources (layouts, images, etc.)
│   └── AndroidManifest.xml
├── build.gradle.kts       # App-level build configuration
└── local.properties       # Local SDK path (tạo nếu chưa có)
```

Sau khi hoàn thành các bước trên, bạn sẽ có thể build và chạy dự án game Android thành công!
