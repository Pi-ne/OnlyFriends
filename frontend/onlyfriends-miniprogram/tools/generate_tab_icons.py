import math
import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "tabbar"
SIZE = 81


def rgba(hex_color, alpha=255):
    hex_color = hex_color.lstrip("#")
    return (
        int(hex_color[0:2], 16),
        int(hex_color[2:4], 16),
        int(hex_color[4:6], 16),
        alpha,
    )


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


def canvas():
    return [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]


def dot(img, cx, cy, r, color):
    for y in range(SIZE):
        for x in range(SIZE):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r ** 2:
                img[y][x] = color


def rect(img, x0, y0, x1, y1, color, radius=0):
    for y in range(max(0, y0), min(SIZE, y1 + 1)):
        for x in range(max(0, x0), min(SIZE, x1 + 1)):
            if radius:
                cx = x0 + radius if x < x0 + radius else x1 - radius if x > x1 - radius else x
                cy = y0 + radius if y < y0 + radius else y1 - radius if y > y1 - radius else y
                if (x - cx) ** 2 + (y - cy) ** 2 > radius ** 2:
                    continue
            img[y][x] = color


def line(img, x0, y0, x1, y1, color, width=5):
    steps = max(abs(x1 - x0), abs(y1 - y0), 1)
    for i in range(steps + 1):
        t = i / steps
        x = round(x0 + (x1 - x0) * t)
        y = round(y0 + (y1 - y0) * t)
        dot(img, x, y, width // 2, color)


def activity(color):
    img = canvas()
    rect(img, 19, 18, 62, 64, color, 9)
    rect(img, 19, 18, 62, 30, color, 9)
    rect(img, 26, 13, 31, 26, color, 2)
    rect(img, 50, 13, 55, 26, color, 2)
    for x in (29, 41, 53):
        for y in (40, 52):
            dot(img, x, y, 3, color)
    return img


def publish(color):
    img = canvas()
    dot(img, 40, 40, 25, color)
    rect(img, 37, 23, 44, 58, (255, 255, 255, 255), 2)
    rect(img, 23, 37, 58, 44, (255, 255, 255, 255), 2)
    return img


def social(color):
    img = canvas()
    dot(img, 33, 31, 10, color)
    dot(img, 52, 35, 8, color)
    rect(img, 18, 47, 48, 64, color, 9)
    rect(img, 43, 50, 64, 63, color, 7)
    return img


def message(color):
    img = canvas()
    rect(img, 14, 17, 67, 58, color, 14)
    line(img, 30, 57, 21, 67, color, 7)
    return img


def profile(color):
    img = canvas()
    dot(img, 40, 28, 12, color)
    rect(img, 21, 48, 60, 66, color, 10)
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    normal = rgba("#77808f")
    active = rgba("#1f7a5a")
    makers = {
        "activity": activity,
        "publish": publish,
        "social": social,
        "message": message,
        "profile": profile,
    }
    for name, maker in makers.items():
        write_png(OUT / f"{name}.png", maker(normal))
        write_png(OUT / f"{name}-active.png", maker(active))


if __name__ == "__main__":
    main()
