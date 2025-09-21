import numpy as np
import matplotlib.pyplot as plt

def carve_rect(grid, r, c, h, w):
    """
    Mở một khu vực hình chữ nhật tại (r,c) với kích thước h x w.
    (r,c) là tọa độ góc trên-trái.
    """
    for i in range(h):
        for j in range(w):
            if 0 <= r + i < grid.shape[0] and 0 <= c + j < grid.shape[1]:
                grid[r + i, c + j] = 0

def carve_winding_path(grid, r1, c1, r2, c2, path_width, rng):
    """
    Tạo một lối đi quanh co (hình chữ Z/U) rộng `path_width` từ (r1,c1) đến (r2,c2).
    Điều này làm tăng chiều dài và độ phức tạp của các hành lang.
    """
    half_w = path_width // 2

    # Ngẫu nhiên quyết định mẫu đường đi: VHV (Dọc-Ngang-Dọc) hoặc HVH (Ngang-Dọc-Ngang)
    if rng.random() > 0.5: # VHV
        # Chọn một hàng trung gian ngẫu nhiên để tạo thành đoạn đường ngang
        if abs(r1 - r2) > path_width * 2:
            ir = rng.integers(min(r1, r2) + path_width, max(r1, r2) - path_width)
        else:
            ir = (r1 + r2) // 2

        # 1. Đoạn dọc từ điểm bắt đầu đến hàng trung gian
        carve_rect(grid, min(r1, ir) - half_w, c1 - half_w, abs(r1 - ir) + path_width, path_width)
        # 2. Đoạn ngang tại hàng trung gian
        carve_rect(grid, ir - half_w, min(c1, c2) - half_w, path_width, abs(c1 - c2) + path_width)
        # 3. Đoạn dọc từ hàng trung gian đến điểm kết thúc
        carve_rect(grid, min(ir, r2) - half_w, c2 - half_w, abs(ir - r2) + path_width, path_width)
    else: # HVH
        # Chọn một cột trung gian ngẫu nhiên
        if abs(c1 - c2) > path_width * 2:
            ic = rng.integers(min(c1, c2) + path_width, max(c1, c2) - path_width)
        else:
            ic = (c1 + c2) // 2
        
        # 1. Đoạn ngang từ điểm bắt đầu đến cột trung gian
        carve_rect(grid, r1 - half_w, min(c1, ic) - half_w, path_width, abs(c1 - ic) + path_width)
        # 2. Đoạn dọc tại cột trung gian
        carve_rect(grid, min(r1, r2) - half_w, ic - half_w, abs(r1 - r2) + path_width, path_width)
        # 3. Đoạn ngang từ cột trung gian đến điểm kết thúc
        carve_rect(grid, r2 - half_w, min(ic, c2) - half_w, path_width, abs(ic - c2) + path_width)

def enforce_door_policy(grid, room_positions, main_path_room_centers, side_path_room_centers, room_size, path_width):
    """
    Xây lại tường cho các phòng và chỉ mở 1 hoặc 2 cửa ra vào.
    - Phòng phụ (side path) có 1 cửa.
    - Phòng chính (main path) có 2 cửa.
    """
    # Tạo một bảng tra cứu từ tọa độ trung tâm về tọa độ góc
    center_to_pos = { (pos[0] + room_size // 2, pos[1] + room_size // 2): pos for pos in room_positions }
    
    all_room_centers = main_path_room_centers + side_path_room_centers

    for center in all_room_centers:
        if center not in center_to_pos: continue
        r, c = center_to_pos[center]
        
        # 1. Tìm tất cả các điểm kết nối từ hành lang bên ngoài
        connections = []
        sides = {
            'top': (r - 1, range(c, c + room_size)),
            'bottom': (r + room_size, range(c, c + room_size)),
            'left': (range(r, r + room_size), c - 1),
            'right': (range(r, r + room_size), c + room_size)
        }

        for side, (row_or_range, col_or_range) in sides.items():
            is_path_segment = False
            segment_start = -1
            # Quét theo chiều ngang (trên, dưới)
            if side in ['top', 'bottom']:
                r_scan = row_or_range
                for c_scan in col_or_range:
                    if 0 <= r_scan < grid.shape[0] and 0 <= c_scan < grid.shape[1] and grid[r_scan, c_scan] == 0:
                        if not is_path_segment:
                            is_path_segment = True
                            segment_start = c_scan
                    elif is_path_segment:
                        is_path_segment = False
                        midpoint = segment_start + (c_scan - 1 - segment_start) // 2
                        connections.append({'side': side, 'pos': midpoint})
                if is_path_segment: # Xử lý nếu lối đi chạm đến cuối phòng
                    midpoint = segment_start + (col_or_range[-1] - segment_start) // 2
                    connections.append({'side': side, 'pos': midpoint})
            # Quét theo chiều dọc (trái, phải)
            else:
                c_scan = col_or_range
                for r_scan in row_or_range:
                    if 0 <= r_scan < grid.shape[0] and 0 <= c_scan < grid.shape[1] and grid[r_scan, c_scan] == 0:
                        if not is_path_segment:
                            is_path_segment = True
                            segment_start = r_scan
                    elif is_path_segment:
                        is_path_segment = False
                        midpoint = segment_start + (r_scan - 1 - segment_start) // 2
                        connections.append({'side': side, 'pos': midpoint})
                if is_path_segment:
                    midpoint = segment_start + (row_or_range[-1] - segment_start) // 2
                    connections.append({'side': side, 'pos': midpoint})

        # 2. Xây lại tường bao quanh phòng
        grid[r, c:c+room_size] = 1 # Tường trên
        grid[r+room_size-1, c:c+room_size] = 1 # Tường dưới
        grid[r:r+room_size, c] = 1 # Tường trái
        grid[r:r+room_size, c+room_size-1] = 1 # Tường phải
        
        # 3. Chọn và đục cửa
        if not connections: continue

        doors_to_carve = []
        if center in side_path_room_centers:
            # Phòng phụ chỉ có 1 cửa
            doors_to_carve.append(connections[0])
        else: # Phòng trên đường chính
            # Ưu tiên chọn 1 cửa ngang và 1 cửa dọc cho đẹp
            h_door = next((conn for conn in connections if conn['side'] in ['top', 'bottom']), None)
            v_door = next((conn for conn in connections if conn['side'] in ['left', 'right']), None)
            
            if h_door and v_door:
                doors_to_carve.append(h_door)
                doors_to_carve.append(v_door)
            elif len(connections) >= 2:
                doors_to_carve.extend(connections[:2]) # Lấy 2 cửa đầu tiên nếu không có đủ ngang/dọc
            else:
                doors_to_carve.append(connections[0]) # Lấy 1 cửa nếu chỉ có 1 kết nối

        # 4. Đục các cửa đã chọn
        half_w = path_width // 2
        for door in doors_to_carve:
            if door['side'] == 'top':
                carve_rect(grid, r, door['pos'] - half_w, 1, path_width)
            elif door['side'] == 'bottom':
                carve_rect(grid, r + room_size - 1, door['pos'] - half_w, 1, path_width)
            elif door['side'] == 'left':
                carve_rect(grid, door['pos'] - half_w, c, path_width, 1)
            elif door['side'] == 'right':
                carve_rect(grid, door['pos'] - half_w, c + room_size - 1, path_width, 1)

def generate_complex_maze_100x100(seed=42):
    """
    Tạo mê cung 100x100 với các phòng có lối vào/ra được kiểm soát.
    - Nền mê cung được tạo với lối đi rộng 4px và tường dày 1px.
    - 4 phòng 10x10. Các phòng trên đường chính có 2 cửa, phòng phụ có 1 cửa.
    - 1 lối vào (trái), 1 lối ra (phải) với tường bao khép kín.
    - Đường đi chính bắt buộc đi qua ít nhất 1 trong 4 phòng.
    """
    # --- Khai báo hằng số và khởi tạo ---
    rng = np.random.default_rng(seed)
    H, W = 100, 100
    PATH_W = 4
    WALL_W = 1
    BLOCK_S = PATH_W + WALL_W
    GRID_H, GRID_W = H // BLOCK_S, W // BLOCK_S
    
    # Lưới pixel, ban đầu tất cả là tường (giá trị 1)
    G = np.ones((H, W), dtype=np.uint8)
    
    # Lưới cell để theo dõi các ô đã được duyệt
    visited = np.zeros((GRID_H, GRID_W), dtype=bool)
    stack = []
    
    # --- Thuật toán tạo mê cung: Randomized DFS ---
    # Bắt đầu từ một ô ngẫu nhiên
    start_r, start_c = rng.integers(0, GRID_H), rng.integers(0, GRID_W)
    stack.append((start_r, start_c))
    visited[start_r, start_c] = True
    
    # Mở đường cho ô đầu tiên
    G[start_r*BLOCK_S : start_r*BLOCK_S + PATH_W, start_c*BLOCK_S : start_c*BLOCK_S + PATH_W] = 0

    while stack:
        cr, cc = stack[-1]  # Ô hiện tại (current row, current col)
        
        # Tìm các ô hàng xóm chưa được duyệt
        neighbors = []
        if cr > 0 and not visited[cr - 1, cc]: neighbors.append((cr - 1, cc, 'U'))
        if cr < GRID_H - 1 and not visited[cr + 1, cc]: neighbors.append((cr + 1, cc, 'D'))
        if cc > 0 and not visited[cr, cc - 1]: neighbors.append((cr, cc - 1, 'L'))
        if cc < GRID_W - 1 and not visited[cr, cc + 1]: neighbors.append((cr, cc + 1, 'R'))
        
        if neighbors:
            # Chọn ngẫu nhiên một hàng xóm
            nr, nc, direction = neighbors[rng.integers(0, len(neighbors))]
            
            # Phá bức tường 1 pixel giữa ô hiện tại và ô hàng xóm
            if direction == 'U': # Tường nằm BÊN DƯỚI ô hàng xóm
                r_wall, c_wall = nr * BLOCK_S + PATH_W, nc * BLOCK_S
                G[r_wall : r_wall + WALL_W, c_wall : c_wall + PATH_W] = 0
            elif direction == 'D': # Tường nằm BÊN TRÊN ô hàng xóm (tức là dưới ô hiện tại)
                r_wall, c_wall = cr * BLOCK_S + PATH_W, cc * BLOCK_S
                G[r_wall : r_wall + WALL_W, c_wall : c_wall + PATH_W] = 0
            elif direction == 'L': # Tường nằm BÊN PHẢI ô hàng xóm
                r_wall, c_wall = nr * BLOCK_S, nc * BLOCK_S + PATH_W
                G[r_wall : r_wall + PATH_W, c_wall : c_wall + WALL_W] = 0
            elif direction == 'R': # Tường nằm BÊN TRÁI ô hàng xóm (tức là bên phải ô hiện tại)
                r_wall, c_wall = cr * BLOCK_S, cc * BLOCK_S + PATH_W
                G[r_wall : r_wall + PATH_W, c_wall : c_wall + WALL_W] = 0
            
            # Mở đường cho ô hàng xóm
            G[nr*BLOCK_S : nr*BLOCK_S + PATH_W, nc*BLOCK_S : nc*BLOCK_S + PATH_W] = 0
            
            visited[nr, nc] = True
            stack.append((nr, nc))
        else:
            # Quay lui nếu không còn hàng xóm nào
            stack.pop()

    # --- CẢI TIẾN: Đảm bảo có tường bao khép kín ---
    # Sau khi tạo mê cung, ta vẽ lại một đường viền dày 1 pixel
    # để đóng mọi lối ra không mong muốn có thể đã được tạo.
    G[0, :] = 1      # Tường trên cùng
    G[H - 1, :] = 1  # Tường dưới cùng
    G[:, 0] = 1      # Tường bên trái
    G[:, W - 1] = 1  # Tường bên phải

    # --- Đặt các phòng và lối đi chính ---
    # Việc này sẽ ghi đè lên mê cung đã tạo, tạo ra các khu vực mở rộng lớn.
    room_size = 10
    path_width = 5 # Sử dụng lại path_width cho lối đi chính và lối vào/ra
    
    room_positions = []
    room_centers = []
    
    # Chia lưới thành 4 góc phần tư để đặt phòng
    quadrants = [
        (1, GRID_H//2 - 3, 1, GRID_W//2 - 3),      # Góc trên-trái
        (1, GRID_H//2 - 3, GRID_W//2 + 1, GRID_W - 3),  # Góc trên-phải
        (GRID_H//2 + 1, GRID_H - 3, GRID_W//2 + 1, GRID_W - 3),  # Góc dưới-phải
        (GRID_H//2 + 1, GRID_H - 3, 1, GRID_W//2 - 3)   # Góc dưới-trái
    ]
    
    for r_min, r_max, c_min, c_max in quadrants:
        # Chọn tọa độ ngẫu nhiên trong góc phần tư, đảm bảo không sát mép
        r_cell = rng.integers(r_min, r_max + 1)
        c_cell = rng.integers(c_min, c_max + 1)
        r, c = r_cell * BLOCK_S, c_cell * BLOCK_S
        
        # Mở phòng (chỉ mở không gian bên trong)
        carve_rect(G, r, c, room_size, room_size)
        
        room_positions.append((r, c))
        room_centers.append((r + room_size // 2, c + room_size // 2))

    # Tạo lối vào và lối ra (đục qua tường bao đã tạo)
    entrance_r = H // 2
    exit_r = H // 2
    carve_rect(G, entrance_r - path_width // 2, 0, path_width, 10)
    carve_rect(G, exit_r - path_width // 2, W - 10, path_width, 10)

    # --- Kết nối các phòng và lối đi để tăng độ phức tạp ---
    start_point = (entrance_r, 10)
    end_point = (exit_r, W - 10)

    # Xáo trộn thứ tự các phòng
    rng.shuffle(room_centers)

    # Quyết định ngẫu nhiên số lượng phòng trên trục đường chính (từ 1 đến 4)
    num_main_rooms = rng.integers(1, 5) 
    main_path_rooms = room_centers[:num_main_rooms]
    side_path_rooms = room_centers[num_main_rooms:]

    # 1. Tạo trục đường chính: Lối vào -> (các phòng chính) -> Lối ra
    main_waypoints = [start_point] + main_path_rooms + [end_point]
    for i in range(len(main_waypoints) - 1):
        r1, c1 = main_waypoints[i]
        r2, c2 = main_waypoints[i+1]
        carve_winding_path(G, r1, c1, r2, c2, path_width, rng)

    # 2. Tạo các nhánh phụ: Nối các phòng còn lại vào mê cung một cách ngẫu nhiên
    # Điều này tạo ra các vòng lặp và nhánh rẽ, tăng độ khó
    all_connection_points = main_waypoints[:] # Sao chép danh sách
    for room_center in side_path_rooms:
        # Chọn một điểm ngẫu nhiên trên đường chính hoặc từ một phòng phụ đã nối để nối vào
        connect_to = all_connection_points[rng.integers(0, len(all_connection_points))]
        carve_winding_path(G, room_center[0], room_center[1], connect_to[0], connect_to[1], path_width, rng)
        # Thêm phòng vừa nối vào danh sách các điểm có thể kết nối tới, tạo thêm sự đa dạng
        all_connection_points.append(room_center)

    # --- Áp dụng chính sách cửa ra vào cho các phòng ---
    enforce_door_policy(G, room_positions, main_path_rooms, side_path_rooms, room_size, path_width)

    # --- Thu thập thông tin trả về ---
    entrance_pixels = (entrance_r - path_width // 2, slice(0, path_width))
    exit_pixels = (exit_r - path_width // 2, slice(W - path_width, W))
    rooms_pix_boxes = [((r, r + room_size), (c, c + room_size)) for (r, c) in room_positions]
    
    return G, entrance_pixels, exit_pixels, rooms_pix_boxes

def save_maze_to_txt(grid, filename="maze_100x100.txt", wall_char="█", path_char=" "):
    """Lưu mê cung ra file txt với ký tự hiển thị."""
    with open(filename, 'w', encoding='utf-8') as f:
        for row in grid:
            line = ''.join(wall_char if cell == 1 else path_char for cell in row)
            f.write(line + '\n')
    print(f"Đã lưu mê cung ra file: {filename}")

def save_maze_to_numeric_txt(grid, filename="maze_numeric.txt"):
    """Lưu mê cung ra file txt với định dạng số: 1=tường, 0=sàn."""
    with open(filename, 'w', encoding='utf-8') as f:
        for row in grid:
            line = ''.join(str(cell) for cell in row)
            f.write(line + '\n')
    print(f"Đã lưu mê cung dạng số ra file: {filename}")

def save_maze_info_to_txt(entrance_pix, exit_pix, rooms, filename="maze_info.txt"):
    """Lưu thông tin mê cung ra file txt."""
    with open(filename, 'w', encoding='utf-8') as f:
        f.write("THÔNG TIN MÊ CUNG 100x100\n")
        f.write("=" * 40 + "\n\n")
        f.write(f"LỐI VÀO: Hàng {entrance_pix[0]} đến {entrance_pix[0]+4}, Cột {entrance_pix[1].start}-{entrance_pix[1].stop-1}\n")
        f.write(f"LỐI RA:  Hàng {exit_pix[0]} đến {exit_pix[0]+4}, Cột {exit_pix[1].start}-{exit_pix[1].stop-1}\n\n")
        f.write("VỊ TRÍ CÁC PHÒNG (góc trên-trái):\n")
        for i, ((r0, r1), (c0, c1)) in enumerate(rooms, 1):
            f.write(f"  Phòng {i}: Hàng {r0}-{r1-1}, Cột {c0}-{c1-1}\n")
    print(f"Đã lưu thông tin mê cung ra file: {filename}")

if __name__ == "__main__":
    # Thay đổi giá trị 'seed' để tạo ra các mê cung khác nhau
    # Cùng một seed sẽ luôn tạo ra cùng một mê cung
    seed_value = 2024
    
    grid, entrance_pix, exit_pix, rooms = generate_complex_maze_100x100(seed=seed_value)
    
    # Lưu mê cung và thông tin ra file
    save_maze_to_txt(grid, "maze_100x100.txt")
    save_maze_to_numeric_txt(grid, "maze_numeric.txt")  # Lưu dạng số
    save_maze_info_to_txt(entrance_pix, exit_pix, rooms, "maze_info.txt")
    
    # Hiển thị mê cung bằng Matplotlib
    try:
        plt.figure(figsize=(8, 8))
        plt.imshow(grid, cmap="binary", interpolation="nearest")
        plt.title(f"Mê cung phức tạp 100x100 (seed={seed_value})")
        plt.axis("off")
        plt.show()
    except ImportError:
        print("Matplotlib không có sẵn, chỉ lưu file txt. Để xem hình ảnh, hãy cài đặt: pip install matplotlib")

