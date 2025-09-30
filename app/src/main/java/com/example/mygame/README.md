# 🎮 Dungeon Crawler Game - Tài liệu dự án

## 📋 Tổng quan dự án
Dự án game Dungeon Crawler được phát triển trên nền tảng Android sử dụng Kotlin. Game có hệ thống điều khiển joystick ảo, background tiling động và camera theo dõi nhân vật.

## 📁 Cấu trúc thư mục chi tiết

### 🎨 **UI Layer (`/ui/`)**
Lớp giao diện người dùng, quản lý tất cả các thành phần UI của ứng dụng.

#### `📱 /ui/activities/`
- **`GameActivity.kt`**
  - **Chức năng**: Activity chính của game, quản lý vòng đời game
  - **Tính năng**:
    - Thiết lập fullscreen mode
    - Ẩn thanh navigation và status bar
    - Giữ màn hình sáng trong khi chơi
    - Xử lý nút back để thoát game
    - Quản lý lifecycle của GameView
  - **Package**: `com.example.mygame.ui.activities`

#### `🖼️ /ui/screens/`
- **`DungeonCrawlerStartScreen.kt`**
  - **Chức năng**: Màn hình chào mừng của game
  - **Tính năng**:
    - Hiển thị background game
    - Nút "START GAME" để bắt đầu
    - Tự động chuyển sang landscape mode
    - UI được thiết kế bằng Jetpack Compose
  - **Package**: `com.example.mygame.ui.screens`

#### `🎨 /ui/theme/`
- **Chức năng**: Quản lý theme và style của ứng dụng
- **Nội dung**: Colors, Typography, Shapes cho UI

---

### 🎯 **Game Layer (`/game/`)**
Lớp logic game, chứa tất cả các thành phần cốt lõi của trò chơi.

#### `🖥️ /game/views/`
- **`GameView.kt`**
  - **Chức năng**: Canvas chính để render game, xử lý input
  - **Tính năng**:
    - **Rendering**: Vẽ background, nhân vật, joystick
    - **Input handling**: Xử lý touch events cho joystick
    - **Game loop**: Chạy game thread với 60 FPS
    - **Camera system**: Camera theo dõi nhân vật
    - **Joystick control**: Điều khiển di chuyển bằng joystick ảo
  - **Kỹ thuật**:
    - SurfaceView để render hiệu suất cao
    - Multi-threading để tránh ANR
    - Optimized drawing chỉ vẽ những gì cần thiết
  - **Package**: `com.example.mygame.game.views`

#### `👤 /game/entities/`
- **`Player.kt`**
  - **Chức năng**: Quản lý nhân vật chính của game
  - **Tính năng**:
    - **Movement**: Di chuyển với 4 hướng (front, back, left, right)
    - **Sprites**: 4 sprite khác nhau cho mỗi hướng
    - **Animation**: Tự động thay đổi sprite theo hướng di chuyển
    - **Collision**: Kiểm tra va chạm với biên thế giới
    - **Position tracking**: Theo dõi vị trí trong world coordinates
  - **Thuộc tính**:
    - Tốc độ di chuyển: 8 pixels/frame
    - Kích thước: 120x120 pixels
    - Sprite files: character_hero_idle_[direction].png
  - **Package**: `com.example.mygame.game.entities`

- **`GameObject.kt`** *(file có sẵn)*
  - **Chức năng**: Base class cho tất cả objects trong game
  - **Sử dụng**: Template cho các entities khác

#### `⚙️ /game/managers/`
- **`BackgroundManager.kt`**
  - **Chức năng**: Quản lý hệ thống background tiling
  - **Tính năng**:
    - **Tiling system**: Ghép background từ nhiều tiles nhỏ
    - **Texture variations**: 6 biến thể từ 1 ảnh gốc:
      - Ảnh gốc
      - Xoay 90°, 180°, 270°
      - Lật ngang, lật dọc
    - **Pattern generation**: Tạo pattern không lặp lại
    - **Camera scrolling**: Background cuộn theo nhân vật
    - **Optimization**: Chỉ render tiles trong viewport
    - **Memory management**: Tự động dọn dẹp bitmap
  - **Kỹ thuật**:
    - Tile size: 256x256 pixels
    - World size: 3000x2000 pixels
    - Viewport culling cho performance
  - **Package**: `com.example.mygame.game.managers`

#### `🚀 /game/systems/` *(sẵn sàng mở rộng)*
- **Chức năng**: Các hệ thống game như Physics, AI, Audio
- **Tương lai**: PhysicsSystem, AISystem, InventorySystem

#### `🎯 /game/core/` *(sẵn sàng mở rộng)*
- **Chức năng**: Core engine và game state management
- **Tương lai**: GameEngine, GameState, SceneManager

---

### 💾 **Data Layer (`/data/`)** *(sẵn sàng mở rộng)*
Lớp dữ liệu, quản lý persistence và data access.
- **Tương lai**: Lưu tiến độ game, high scores, settings

### 🏢 **Domain Layer (`/domain/`)** *(sẵn sàng mở rộng)*
Lớp business logic, chứa các use cases và domain models.
- **Tương lai**: Game rules, scoring logic, level progression

### 🔧 **Utils (`/utils/`)** *(sẵn sàng mở rộng)*
Các utility functions và helper classes.
- **Tương lai**: Math helpers, extension functions, constants

---

## 🎮 **Luồng hoạt động của Game**

### 1. **Khởi động ứng dụng**
```
MainActivity → DungeonCrawlerStartScreen → Hiển thị menu chính
```

### 2. **Bắt đầu game**
```
Nhấn "START GAME" → MainActivity.startActivity(GameActivity) → GameActivity.onCreate()
```

### 3. **Khởi tạo game**
```
GameActivity → Tạo GameView → GameView.surfaceCreated() → 
Khởi tạo Player & BackgroundManager → Bắt đầu GameThread
```

### 4. **Game loop (60 FPS)**
```
GameThread.run() → GameView.update() → GameView.renderGame() → 
Update Player position → Update Camera → Render background & player & joystick
```

### 5. **Input handling**
```
Touch event → GameView.onTouchEvent() → Joystick calculation → 
Player.setMovementDirection() → Update velocity
```

### 6. **Thoát game**
```
Back button → GameActivity.onBackPressed() → GameView.onPause() → 
Stop GameThread → Return to MainActivity
```

---

## 🎯 **Tính năng hiện tại**

### ✅ **Đã hoàn thành**
- [x] Hệ thống joystick ảo
- [x] Nhân vật di chuyển 4 hướng với sprite animation
- [x] Background tiling với 6 biến thể texture
- [x] Camera theo dõi nhân vật
- [x] Thế giới mở rộng (3000x2000)
- [x] Fullscreen gameplay
- [x] Performance optimization (60 FPS)
- [x] Memory management
- [x] ANR prevention

### 🔄 **Có thể mở rộng**
- [ ] Enemies và AI system
- [ ] Collision detection với objects
- [ ] Sound effects và background music
- [ ] Multiple levels/scenes
- [ ] Inventory system
- [ ] Combat system
- [ ] Save/Load game state
- [ ] Settings menu
- [ ] High score system

---

## 🛠️ **Kỹ thuật sử dụng**

### **Android Technologies**
- **Canvas 2D**: Rendering game graphics
- **SurfaceView**: High-performance drawing surface
- **Multi-threading**: Game loop chạy trên background thread
- **Activity lifecycle**: Proper pause/resume handling

### **Game Development Patterns**
- **Component-based architecture**: Entities có các components riêng
- **Manager pattern**: BackgroundManager, AudioManager (future)
- **MVC pattern**: Separation between game logic và rendering
- **Object pooling**: Memory efficient object reuse (future)

### **Performance Optimizations**
- **Viewport culling**: Chỉ render objects trong màn hình
- **Bitmap recycling**: Giải phóng memory khi không dùng
- **Frame rate limiting**: Stable 60 FPS
- **Efficient collision detection**: Spatial partitioning (future)

---

## 📱 **Yêu cầu hệ thống**
- **Android**: API level 21+ (Android 5.0)
- **RAM**: Tối thiểu 2GB
- **Storage**: 50MB trống
- **Screen**: Hỗ trợ landscape orientation

---

## 👥 **Credits**
- **Developer**: L02 KMA Team
- **Art Assets**: Character sprites, background textures
- **Engine**: Custom Android 2D game engine

---

*Cập nhật lần cuối: 12/09/2025*