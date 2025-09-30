# Hướng dẫn tổ chức tài nguyên Game Assets

## 📁 CẤU TRÚC THỬ MỤC TÀI NGUYÊN:

### 🎮 **app/src/main/assets/** - CHỦ YẾU CHO GAME ASSETS:
```
assets/
├── textures/          # Tất cả hình ảnh game
│   ├── characters/    # Sprites nhân vật (player, enemy, NPC)
│   ├── environment/   # Background, tiles, dungeon walls/floors
│   ├── items/         # Weapons, armor, potions, treasures
│   └── ui/           # Buttons, panels, icons, HUD elements
├── sounds/           # Sound effects (.wav, .ogg)
├── music/            # Background music (.mp3, .ogg)
├── fonts/            # Custom fonts (.ttf, .otf)
└── data/             # Game data (JSON, XML configs)
```

### 🎨 **app/src/main/res/** - VẪN CẦN CHO ANDROID UI:
```
res/
├── drawable/         # UI backgrounds, app icons, simple graphics
├── mipmap-*/         # App launcher icons (các độ phân giải)
├── values/           # Colors, strings, styles, themes
└── xml/              # Vector drawables, animations
```

## 🔄 **PHÂN BIỆT KHI NÀO DÙNG GÌ:**

### **DÙNG res/drawable/ KHI:**
- ✅ Background cho màn hình menu/UI
- ✅ Button backgrounds và UI elements
- ✅ App icons và launcher icons
- ✅ Simple graphics không cần load động
- ✅ Vector drawables (SVG-like)

### **DÙNG assets/ KHI:**
- ✅ Game sprites và textures
- ✅ Animations và sprite sheets
- ✅ Large images/textures
- ✅ Audio files (sounds, music)
- ✅ Custom fonts
- ✅ Game data files (JSON, configs)
- ✅ Bất kỳ file nào cần load động trong game

## 📋 **HƯỚNG DẪN CHI TIẾT:**

### **1. Textures (Hình ảnh game):**
```
assets/textures/characters/
├── player_idle.png
├── player_walk_01.png
├── player_walk_02.png
├── enemy_orc.png
└── npc_merchant.png

assets/textures/environment/
├── dungeon_wall.png
├── dungeon_floor.png
├── background_forest.png
└── tiles_stone.png

assets/textures/items/
├── sword_iron.png
├── shield_wooden.png
├── potion_health.png
└── treasure_chest.png
```

### **2. Audio:**
```
assets/sounds/
├── attack_sword.wav
├── footstep.wav
├── item_pickup.wav
└── ui_button_click.wav

assets/music/
├── menu_theme.mp3
├── dungeon_ambient.mp3
└── battle_theme.mp3
```

### **3. Fonts:**
```
assets/fonts/
├── game_title.ttf
└── game_ui.ttf
```

### **4. Data files:**
```
assets/data/
├── levels.json
├── items_config.json
└── enemies_stats.json
```

## 💡 **LỢI ÍCH CỦA CÁCH TỔ CHỨC NÀY:**

### **Assets folder:**
- ✅ **File size linh hoạt**: Không bị nén tự động
- ✅ **Load động**: Có thể load/unload theo nhu cầu
- ✅ **Performance tốt**: Phù hợp cho game assets lớn
- ✅ **Tổ chức rõ ràng**: Dễ quản lý theo loại tài nguyên

### **Res folder:**
- ✅ **Android optimization**: Tự động tối ưu cho UI
- ✅ **Multi-density support**: Tự động scale theo màn hình
- ✅ **Easy access**: Dễ dàng reference trong code
- ✅ **Theme support**: Hỗ trợ dark/light theme

## 🚀 **CODE MẪU SỬ DỤNG:**

### **Load texture từ assets:**
```kotlin
// In AssetManager class
fun loadTexture(path: String): Bitmap {
    return context.assets.open("textures/$path").use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    }
}

// Usage
val playerSprite = assetManager.loadTexture("characters/player_idle.png")
```

### **Load sound từ assets:**
```kotlin
// In SoundManager class
fun loadSound(path: String): Int {
    return soundPool.load(context.assets.openFd("sounds/$path"), 1)
}

// Usage
val attackSoundId = soundManager.loadSound("attack_sword.wav")
```

### **Sử dụng drawable từ res:**
```kotlin
// In Compose UI
Image(
    painter = painterResource(id = R.drawable.background),
    contentDescription = "Background"
)
```
