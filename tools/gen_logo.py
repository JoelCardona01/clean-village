#!/usr/bin/env python3
"""Draws the Clean Village logo, banner and mod icon (original pixel art, no Mojang assets)."""
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "media"
OUT.mkdir(exist_ok=True)

FONT = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "C": ["01111", "10000", "10000", "10000", "10000", "10000", "01111"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "I": ["111", "010", "010", "010", "010", "010", "111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
    "G": ["01111", "10000", "10000", "10011", "10001", "10001", "01111"],
    "N": ["10001", "11001", "11001", "10101", "10011", "10011", "10001"],
    " ": ["000", "000", "000", "000", "000", "000", "000"],
}


def text_mask(text, scale=1, spacing=1):
    cols = []
    for ch in text:
        g = FONT[ch]
        w = len(g[0])
        for x in range(w):
            cols.append([g[y][x] == "1" for y in range(7)])
        cols.append([False] * 7)
    w = len(cols) * scale
    img = Image.new("L", (w, 7 * scale), 0)
    for x, col in enumerate(cols):
        for y, on in enumerate(col):
            if on:
                for dx in range(scale):
                    for dy in range(scale):
                        img.putpixel((x * scale + dx, y * scale + dy), 255)
    return img


def draw_text(canvas, text, x, y, scale, fill, outline, shadow=None):
    m = text_mask(text, scale)
    if shadow:
        canvas.paste(Image.new("RGBA", m.size, shadow), (x + 2, y + 2), m)
    for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, 1), (-1, 1), (1, -1)):
        canvas.paste(Image.new("RGBA", m.size, outline), (x + dx, y + dy), m)
    canvas.paste(Image.new("RGBA", m.size, fill), (x, y), m)
    # top highlight row on each glyph
    hl = Image.new("L", m.size, 0)
    for xx in range(m.width):
        for yy in range(m.height):
            if m.getpixel((xx, yy)) and (yy == 0 or not m.getpixel((xx, yy - 1))):
                hl.putpixel((xx, yy), 255)
    canvas.paste(Image.new("RGBA", m.size, (255, 255, 255, 255)), (x, y), hl)
    return m.width


def house(c, x0, y0, rnd):
    """24x22 pixel house: left half dirty, right half sparkling."""
    d = ImageDraw.Draw(c)
    wall, wall_d, log = (196, 150, 92, 255), (158, 116, 66, 255), (110, 78, 44, 255)
    roof, roof_d = (150, 64, 52, 255), (112, 44, 38, 255)
    # roof
    for i in range(8):
        d.line([(x0 + 1 + i, y0 + 7 - i), (x0 + 26 - i, y0 + 7 - i)], fill=roof if i % 2 else roof_d)
    d.rectangle([x0 + 3, y0 + 8, x0 + 24, y0 + 22], fill=wall)
    for yy in range(y0 + 8, y0 + 23, 3):
        d.line([(x0 + 3, yy), (x0 + 24, yy)], fill=wall_d)
    d.rectangle([x0 + 3, y0 + 8, x0 + 3, y0 + 22], fill=log)
    d.rectangle([x0 + 24, y0 + 8, x0 + 24, y0 + 22], fill=log)
    # door + windows
    d.rectangle([x0 + 12, y0 + 15, x0 + 15, y0 + 22], fill=(96, 62, 30, 255))
    d.point((x0 + 14, y0 + 19), fill=(230, 200, 80, 255))
    d.rectangle([x0 + 5, y0 + 11, x0 + 9, y0 + 14], fill=(120, 110, 80, 255))      # dirty window
    d.rectangle([x0 + 18, y0 + 11, x0 + 22, y0 + 14], fill=(170, 225, 255, 255))   # clean window
    d.line([(x0 + 18, y0 + 14), (x0 + 21, y0 + 11)], fill=(255, 255, 255, 255))
    # dirt on the left half
    for _ in range(70):
        px, py = x0 + 3 + rnd.randrange(10), y0 + 8 + rnd.randrange(15)
        if rnd.random() < 0.7:
            d.point((px, py), fill=(88, 64, 40, 255) if rnd.random() < 0.6 else (72, 110, 46, 255))
    for i in range(3):  # cobweb corner
        d.point((x0 + 4 + i, y0 + 9), fill=(220, 220, 220, 255))
        d.point((x0 + 4, y0 + 9 + i), fill=(220, 220, 220, 255))
    d.point((x0 + 5, y0 + 10), fill=(220, 220, 220, 255))
    # sparkles on the right
    for (sx, sy) in ((x0 + 26, y0 + 10), (x0 + 21, y0 + 17), (x0 + 29, y0 + 3)):
        sparkle(d, sx, sy)


def sparkle(d, x, y, big=False):
    col = (255, 255, 255, 255)
    d.point((x, y), fill=col)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        d.point((x + dx, y + dy), fill=(255, 245, 170, 255))
    if big:
        for dx, dy in ((2, 0), (-2, 0), (0, 2), (0, -2)):
            d.point((x + dx, y + dy), fill=(255, 245, 170, 200))


def bubble(d, x, y, r):
    d.ellipse([x - r, y - r, x + r, y + r], outline=(200, 236, 255, 255))
    d.point((x - r // 2, y - r // 2), fill=(255, 255, 255, 255))


def background(w, h):
    c = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(c)
    for y in range(h):
        t = y / h
        d.line([(0, y), (w, y)], fill=(int(96 + 70 * t), int(170 + 50 * t), int(240 - 10 * t), 255))
    return c


def banner():
    W, H, S = 200, 80, 8
    rnd = random.Random(3)
    c = background(W, H)
    d = ImageDraw.Draw(c)
    # clouds
    for cx, cy in ((20, 10), (150, 6), (178, 22)):
        d.rectangle([cx, cy, cx + 14, cy + 3], fill=(255, 255, 255, 230))
        d.rectangle([cx + 3, cy - 2, cx + 10, cy], fill=(255, 255, 255, 230))
    # grass
    d.rectangle([0, 66, W, H], fill=(94, 157, 52, 255))
    for x in range(0, W, 2):
        d.point((x, 66 + (x * 7) % 3), fill=(120, 190, 70, 255))
    d.rectangle([0, 72, W, H], fill=(134, 96, 67, 255))
    house(c, 8, 44, rnd)
    # title
    draw_text(c, "CLEAN", 50, 12, 3, (255, 255, 255, 255), (30, 60, 110, 255), (20, 40, 80, 160))
    draw_text(c, "VILLAGE", 50, 40, 3, (120, 220, 255, 255), (30, 60, 110, 255), (20, 40, 80, 160))
    for (bx, by, r) in ((44, 12, 2), (196, 64, 2), (192, 8, 2)):
        bubble(d, bx, by, r)
    sparkle(d, 196, 36, True)
    sparkle(d, 46, 38)
    img = c.resize((W * S, H * S), Image.NEAREST)
    img.save(OUT / "banner.png")
    return img


def icon():
    W, S = 32, 16
    rnd = random.Random(7)
    c = background(W, W)
    d = ImageDraw.Draw(c)
    d.rectangle([0, 26, W, W], fill=(94, 157, 52, 255))
    house(c, 1, 4, rnd)
    bubble(d, 27, 22, 2)
    bubble(d, 4, 3, 1)
    img = c.resize((W * S, W * S), Image.NEAREST)
    img.save(OUT / "icon.png")
    small = c.resize((128, 128), Image.NEAREST)
    p = ROOT / "common/src/main/resources/assets/cleanvillage/icon.png"
    small.save(p)
    return img


if __name__ == "__main__":
    banner()
    icon()
    print("logo written to", OUT)
