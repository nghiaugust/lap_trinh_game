# Cấu trúc thư mục Game 2D - Hướng dẫn tổ chức

## Cấu trúc thư mục đã được tổ chức theo Clean Architecture:

### 📁 ui/ - Giao diện người dùng
- **screens/**: Chứa các màn hình game (MenuScreen, GameplayScreen, SettingsScreen)
- **components/**: Chứa các UI component có thể tái sử dụng (GameButton, HealthBar, Dialog)
- **theme/**: Chứa theme, colors, typography của game

### 📁 data/ - Tầng dữ liệu
- **models/**: Chứa data models (PlayerData, EnemyData, ItemData, GameState)
- **repository/**: Chứa repository pattern cho việc truy cập dữ liệu (GameRepository, SaveRepository)

### 📁 domain/ - Tầng business logic
- **entities/**: Chứa domain entities (Player, Enemy, Item, Game)
- **usecases/**: Chứa use cases (AttackUseCase, MovePlayerUseCase, SaveGameUseCase)

### 📁 game/ - Game engine và logic
- **engine/**: Core game engine (GameLoop, Renderer, InputHandler, PhysicsEngine)
- **entities/**: Game entities (GameObject, Sprite, Animation, Collider)
- **systems/**: Game systems (CombatSystem, MovementSystem, AISystem, PhysicsSystem)
- **scenes/**: Game scenes/levels (MenuScene, DungeonScene, BattleScene)
- **assets/**: Asset management (TextureManager, SoundManager, AssetLoader)

### 📁 utils/ - Tiện ích
- Constants, Extensions, Helper functions, Math utilities

## 🎮 TỔ CHỨC TÀI NGUYÊN GAME:

### 📁 app/src/main/assets/ - TÀI NGUYÊN GAME CHÍNH:
```
assets/
├── textures/          # Tất cả hình ảnh game
│   ├── characters/    # Player, enemies, NPCs sprites
│   ├── environment/   # Backgrounds, tiles, dungeon elements
│   ├── items/         # Weapons, armor, potions, treasures
│   └── ui/           # Game UI elements
├── sounds/           # Sound effects (.wav, .ogg)
├── music/            # Background music (.mp3, .ogg)
├── fonts/            # Custom fonts (.ttf, .otf)
└── data/             # Game configurations (JSON, XML)
```

### 📁 app/src/main/res/ - ANDROID UI TÀI NGUYÊN:
```
res/
├── drawable/         # UI backgrounds, app icons
├── mipmap-*/         # App launcher icons
├── values/           # Colors, strings, styles
└── xml/              # Vector drawables, animations
```

## Hướng dẫn sử dụng:

1. **UI Layer**: Tất cả giao diện Jetpack Compose
2. **Data Layer**: Lưu trữ và truy xuất dữ liệu
3. **Domain Layer**: Business logic không phụ thuộc framework
4. **Game Layer**: Core game logic và engine
5. **Utils**: Các function tiện ích dùng chung
6. **Assets**: Tài nguyên game (textures, sounds, music, data)
7. **Res**: Tài nguyên Android UI (drawables, values, themes)

### **SỬ DỤNG assets/ CHO:**
- ✅ Game sprites và animations
- ✅ Sound effects và background music
- ✅ Custom fonts cho game
- ✅ Game data files (levels, configs)
- ✅ Large textures và images
- ✅ Bất kỳ tài nguyên nào cần load động

### **SỬ DỤNG res/ CHO:**
- ✅ App icons và launcher icons
- ✅ UI backgrounds cho screens
- ✅ Button styles và UI themes
- ✅ Colors, strings, dimensions
- ✅ Vector drawables (simple graphics)
- ✅ Android system resources

