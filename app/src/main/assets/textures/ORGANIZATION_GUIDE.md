# Game Assets Organization Guide

## 📁 Cấu trúc thư mục hoàn chỉnh đã tạo:

```
assets/textures/
├── characters/
│   ├── player/
│   │   ├── idle/          # Player đứng yên
│   │   ├── walk/          # Player di chuyển
│   │   ├── attack/        # Player tấn công
│   │   └── death/         # Player chết
│   ├── enemies/
│   │   ├── orc/           # Quái orc
│   │   ├── skeleton/      # Quái xương
│   │   └── boss/          # Boss enemies
│   └── npcs/
│       ├── merchant/      # Thương gia
│       └── guard/         # Lính gác
├── environment/
│   ├── tiles/             # Tiles cơ bản
│   ├── walls/             # Tường
│   ├── floors/            # Sàn
│   ├── backgrounds/       # Background lớn
│   └── objects/           # Đối tượng môi trường
├── items/
│   ├── weapons/
│   │   ├── swords/        # Kiếm
│   │   ├── axes/          # Rìu
│   │   └── bows/          # Cung
│   ├── armor/
│   │   ├── helmets/       # Mũ
│   │   ├── chest/         # Giáp ngực
│   │   └── shields/       # Khiên
│   ├── consumables/
│   │   ├── potions/       # Thuốc
│   │   └── food/          # Đồ ăn
│   └── treasures/         # Kho báu
├── ui/
│   ├── buttons/           # Nút bấm
│   ├── panels/            # Bảng
│   ├── icons/             # Icons
│   └── hud/               # HUD elements
└── effects/
    ├── particles/         # Particle effects
    ├── magic/             # Magic effects
    └── combat/            # Combat effects
```

## 🎯 Cách sử dụng:

1. **Đặt ảnh** vào thư mục tương ứng
2. **Đặt tên** theo convention trong README.md của từng thư mục
3. **Load trong code** bằng GameAssetManager

## 📏 Kích thước khuyến nghị:

- **Characters**: 64x64 hoặc 128x128
- **Tiles**: 64x64
- **Items**: 32x32 hoặc 64x64
- **UI**: Tuỳ theo element
- **Effects**: 64x64 hoặc 128x128

## 🔧 Integration với code:

Check `GameAssetManager.kt` để load assets từ các thư mục này.
