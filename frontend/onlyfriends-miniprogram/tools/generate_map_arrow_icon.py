import math
import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "icons"
SIZE = 96


def write_png(path, pixels):
    raw = bytearray()
    for y in range(SIZE):
        raw.append(0)
        for x in range(SIZE):
            raw.extend(pixels[y][x])

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    data = bytearray(b"\x89PNG\r\n\x1a\n")
    data.extend(chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)))
    data.extend(chunk(b"IDAT", zlib.compress(bytes(raw), 9)))
    data.extend(chunk(b"IEND", b""))
    path.write_bytes(data)


def blend(dst, src):
    sr, sg, sb, sa = src
    if sa == 255:
        return src
    if sa == 0:
        return dst
    dr, dg, db, da = dst
    a = sa / 255
    out_a = a + da / 255 * (1 - a)
    if out_a == 0:
        return (0, 0, 0, 0)
    return (
        round((sr * a + dr * da / 255 * (1 - a)) / out_a),
        round((sg * a + dg * da / 255 * (1 - a)) / out_a),
        round((sb * a + db * da / 255 * (1 - a)) / out_a),
        round(out_a * 255),
    )


def draw_circle(img, cx, cy, r, color):
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if d <= r:
                img[y][x] = color
            elif d <= r + 1.5:
                alpha = max(0, min(255, round((r + 1.5 - d) / 1.5 * color[3])))
                img[y][x] = blend(img[y][x], (*color[:3], alpha))


def point_in_poly(x, y, pts):
    inside = False
    j = len(pts) - 1
    for i, (xi, yi) in enumerate(pts):
        xj, yj = pts[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            inside = not inside
        j = i
    return inside


def draw_poly(img, pts, color):
    min_x = max(0, math.floor(min(x for x, _ in pts)) - 2)
    max_x = min(SIZE - 1, math.ceil(max(x for x, _ in pts)) + 2)
    min_y = max(0, math.floor(min(y for _, y in pts)) - 2)
    max_y = min(SIZE - 1, math.ceil(max(y for _, y in pts)) + 2)
    for y in range(min_y, max_y + 1):
        for x in range(min_x, max_x + 1):
            if point_in_poly(x + 0.5, y + 0.5, pts):
                img[y][x] = blend(img[y][x], color)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    img = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]

    draw_circle(img, 48, 48, 37, (48, 149, 246, 255))
    draw_circle(img, 48, 48, 31, (34, 136, 238, 255))

    # Navigation arrow: broad tail, sharp north-east tip, subtly like a map app locator.
    arrow = [(48, 18), (69, 73), (50, 62), (31, 73)]
    draw_poly(img, arrow, (255, 255, 255, 255))
    inner_shadow = [(48, 28), (58, 59), (49, 53), (38, 59)]
    draw_poly(img, inner_shadow, (214, 235, 255, 255))

    write_png(OUT / "map-arrow.png", img)


if __name__ == "__main__":
    main()
