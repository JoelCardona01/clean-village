#!/usr/bin/env python3
"""
Builds the store media (JPG stills, GIFs and an MP4 trailer) from raw in-game captures.

Usage: python3 tools/make_media.py <captures dir> [out dir=media]
The captures dir holds the PNGs taken with the dev script runner (see devtools/DevScript.java):
  stills  s1_exterior, cal_sad_h2 (before), s3_scrubbing_ext, s4_waiting, s5b_before, s6_clean_inside,
          s7_clean_outside, s8_portrait
  bursts  g_scrub_###.png (2-tick interval, ~10 fps), g_celeb_###.png (3-tick interval, ~6.7 fps); both are motion-interpolated to 20/30 fps
"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFont

SRC = Path(sys.argv[1])
OUT = Path(sys.argv[2] if len(sys.argv) > 2 else Path(__file__).resolve().parent.parent / "media")
OUT.mkdir(parents=True, exist_ok=True)
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
W, H = 1920, 1080


def load(name, size=(W, H)):
    im = Image.open(SRC / f"{name}.png").convert("RGB")
    return im.resize(size, Image.LANCZOS)


def grade(im):
    """Light polish: a bit more contrast and saturation, like a typical showcase shot."""
    im = ImageEnhance.Contrast(im).enhance(1.06)
    return ImageEnhance.Color(im).enhance(1.12)


def label(im, text, corner="bl", size=64, pad=36):
    d = ImageDraw.Draw(im, "RGBA")
    f = ImageFont.truetype(BOLD, size)
    x0, y0, x1, y1 = d.textbbox((0, 0), text, font=f)
    w, h = x1 - x0, y1 - y0
    x = pad if "l" in corner else im.width - w - pad - 28
    y = im.height - h - pad - 28 if "b" in corner else pad
    d.rounded_rectangle([x - 18, y - 14, x + w + 18, y + h + 24], 18, fill=(0, 0, 0, 150))
    d.text((x, y - y0 + 4), text, font=f, fill=(255, 255, 255, 255))
    return im


def save_jpg(im, name):
    im.save(OUT / name, quality=90, optimize=True, progressive=True)
    print("wrote", name, im.size)


def stills():
    pairs = {
        "01_dirty_house.jpg": "s1_exterior",
        "02_sad_villager.jpg": "cal_sad_h2",
        "03_scrubbing.jpg": "g_scrub_075",
        "04_waiting_at_door.jpg": "s4_waiting",
        "06_clean_house.jpg": "s6_clean_inside",
    }
    for out, src in pairs.items():
        if (SRC / f"{src}.png").exists():
            save_jpg(grade(load(src)), out)
    if (SRC / "s8_portrait.png").exists():  # the portrait is small in frame: crop around it
        im = Image.open(SRC / "s8_portrait.png").convert("RGB")
        cw = int(im.width * 0.36); ch = cw * 9 // 16; cx, cy = im.width // 2, im.height // 2
        save_jpg(grade(im.crop((cx - cw // 2, cy - ch // 2, cx + cw // 2, cy + ch // 2)).resize((W, H), Image.NEAREST)),
                 "07_family_portrait.jpg")
    cel = "g_celeb_060" if (SRC / "g_celeb_060.png").exists() else best_celebration_frame()
    if cel:
        save_jpg(grade(load(cel)), "05_celebration.jpg")
    # before / after, side by side
    for out, a, b in (("before_after.jpg", "cal_sad_h2", "s6_clean_inside"),
                      ("before_after_outside.jpg", "s1_exterior", "s7_clean_outside")):
        if not ((SRC / f"{a}.png").exists() and (SRC / f"{b}.png").exists()):
            continue
        A, B = grade(load(a, (960, 540))), grade(load(b, (960, 540)))
        canvas = Image.new("RGB", (1928, 540), (255, 255, 255))
        canvas.paste(label(A, "BEFORE", size=44, pad=24), (0, 0))
        canvas.paste(label(B, "AFTER", size=44, pad=24), (968, 0))
        save_jpg(canvas, out)


def frames(prefix):
    return sorted(SRC.glob(f"{prefix}_[0-9][0-9][0-9].png"))


def best_celebration_frame():
    fs = frames("g_celeb")
    if not fs:
        return None
    # the flash with sparkles happens shortly after the start: pick the frame with most bright pixels
    best, score = None, -1
    for f in fs[:60]:
        im = Image.open(f).convert("L").resize((240, 135))
        s = sum(1 for p in im.tobytes() if p > 235)
        if s > score:
            best, score = f.stem, s
    return best


def smooth_frames(prefix, tick_interval, fps, width, start=0, end=None, caption=None):
    """Motion-interpolates a capture burst (taken every `tick_interval` ticks = 20/tick_interval fps)
    up to `fps`, so playback is fluid instead of a slideshow. Returns a dir with %04d.png frames."""
    fs = frames(prefix)[start:end]
    h = width * 9 // 16
    src, dst = Path(tempfile.mkdtemp()), Path(tempfile.mkdtemp())
    for i, f in enumerate(fs):
        im = grade(Image.open(f).convert("RGB").resize((width, h), Image.LANCZOS))
        if caption:
            label(im, caption, size=max(18, width // 32), pad=width // 53)
        im.save(src / f"{i:04d}.png")
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-framerate", str(20 / tick_interval), "-i", str(src / "%04d.png"),
                    "-vf", f"minterpolate=fps={fps}:mi_mode=mci:mc_mode=aobmc:me_mode=bidir:vsbmc=1",
                    str(dst / "%04d.png")], check=True)
    shutil.rmtree(src)
    return dst


def gif_from_dir(d, out, fps, width):
    pal = d / "pal.png"
    vf = f"scale={width}:-1:flags=lanczos"
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-framerate", str(fps), "-i", str(d / "%04d.png"),
                    "-vf", f"{vf},palettegen=stats_mode=diff:max_colors=96", str(pal)], check=True)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-framerate", str(fps), "-i", str(d / "%04d.png"), "-i", str(pal),
                    "-lavfi", f"{vf}[x];[x][1:v]paletteuse=dither=bayer:bayer_scale=3:diff_mode=rectangle",
                    "-loop", "0", str(OUT / out)], check=True)
    print("wrote", out, f"{(OUT / out).stat().st_size // 1024} KiB")


def gif(prefix, out, width=720, fps=15, step=1, start=0, end=None, speed=1.0, caption=None):
    fs = frames(prefix)[start:end:step]
    if not fs:
        print("no frames for", prefix)
        return None
    tmp = Path(tempfile.mkdtemp())
    for i, f in enumerate(fs):
        im = grade(Image.open(f).convert("RGB").resize((W, H), Image.LANCZOS))
        if caption:
            label(im, caption, size=60)
        im.save(tmp / f"{i:04d}.png")
    pal = tmp / "pal.png"
    rate = fps * speed
    vf = f"scale={width}:-1:flags=lanczos"
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-framerate", str(rate), "-i", str(tmp / "%04d.png"),
                    "-vf", f"{vf},palettegen=stats_mode=diff:max_colors=96", str(pal)], check=True)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-framerate", str(rate), "-i", str(tmp / "%04d.png"), "-i", str(pal),
                    "-lavfi", f"{vf}[x];[x][1:v]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle",
                    "-loop", "0", str(OUT / out)], check=True)
    print("wrote", out, f"{(OUT / out).stat().st_size // 1024} KiB", len(fs), "frames")
    return tmp


def wipe_frames(a, b, n=36, hold=18):
    """Before/after slider: hold BEFORE, sweep a divider across, hold AFTER, sweep back."""
    A, B = grade(load(a)), grade(load(b))
    label(A, "BEFORE"), label(B, "AFTER", corner="br")
    out = [A] * hold
    for i in range(1, n + 1):
        t = i / n
        t = t * t * (3 - 2 * t)
        x = int(W * t)
        im = A.copy()
        im.paste(B.crop((0, 0, x, H)), (0, 0))
        ImageDraw.Draw(im).rectangle([x - 4, 0, x + 4, H], fill=(255, 255, 255))
        out.append(im)
    out += [B] * hold
    return out


def write_seq(images, prefix):
    tmp = Path(tempfile.mkdtemp())
    for i, im in enumerate(images):
        if isinstance(im, Path):
            shutil.copy(im, tmp / f"{prefix}{i:04d}.png")
        else:
            im.save(tmp / f"{prefix}{i:04d}.png")
    return tmp


def before_after_gif():
    if not ((SRC / "cal_sad_h2.png").exists() and (SRC / "s6_clean_inside.png").exists()):
        return
    seq = wipe_frames("cal_sad_h2", "s6_clean_inside")
    seq = seq + list(reversed(seq))
    tmp = write_seq(seq, "f")
    pal = tmp / "pal.png"
    vf = "scale=720:-1:flags=lanczos"
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-framerate", "24", "-i", str(tmp / "f%04d.png"),
                    "-vf", f"{vf},palettegen=max_colors=200", str(pal)], check=True)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-framerate", "24", "-i", str(tmp / "f%04d.png"), "-i", str(pal),
                    "-lavfi", f"{vf}[x];[x][1:v]paletteuse=dither=bayer:bayer_scale=4", "-loop", "0",
                    str(OUT / "before_after.gif")], check=True)
    print("wrote before_after.gif", (OUT / "before_after.gif").stat().st_size // 1024, "KiB")
    shutil.rmtree(tmp)


def title_card(text, sub=None):
    bg = Image.open(OUT / "banner.png").convert("RGB").resize((W, int(W * 640 / 1600)), Image.NEAREST)
    im = Image.new("RGB", (W, H), (96, 168, 232))
    im.paste(bg, (0, (H - bg.height) // 2))
    d = ImageDraw.Draw(im, "RGBA")
    if text:
        f = ImageFont.truetype(BOLD, 54)
        w = d.textlength(text, font=f)
        d.rounded_rectangle([(W - w) / 2 - 30, H - 190, (W + w) / 2 + 30, H - 105], 20, fill=(0, 0, 0, 140))
        d.text(((W - w) / 2, H - 182), text, font=f, fill="white")
    if sub:
        f = ImageFont.truetype(BOLD, 36)
        w = d.textlength(sub, font=f)
        d.text(((W - w) / 2, H - 92), sub, font=f, fill=(255, 255, 255))
    return im


def ken_burns(name, seconds, caption, fps=30, zoom=1.08):
    base = grade(load(name, (int(W * zoom) + 2, int(H * zoom) + 2)))
    out = []
    n = int(seconds * fps)
    for i in range(n):
        t = i / max(1, n - 1)
        z = 1 + (zoom - 1) * t
        cw, ch = int(base.width / z), int(base.height / z)
        x, y = (base.width - cw) // 2, (base.height - ch) // 2
        im = base.crop((x, y, x + cw, y + ch)).resize((W, H), Image.LANCZOS)
        out.append(label(im, caption, size=58))
    return out


def burst_clip(prefix, caption, tick_interval, fps=30, start=0, end=None):
    fs = frames(prefix)[start:end]
    out = []
    repeat = max(1, round(fps * tick_interval / 20))
    for f in fs:
        im = label(grade(Image.open(f).convert("RGB").resize((W, H), Image.LANCZOS)), caption, size=58)
        out += [im] * repeat
    return out


def smooth_clip(prefix, caption, tick_interval, start=0, end=None):
    d = smooth_frames(prefix, tick_interval, 30, W, start, end, caption)
    return sorted(d.glob("[0-9]*.png"))  # paths, to keep memory low (loaded in write_seq)


def trailer():
    seq = []
    seq += [title_card("Some village houses are filthy…")] * 75
    if (SRC / "s1_exterior.png").exists():
        seq += ken_burns("s1_exterior", 3.0, "A house nobody wants to enter")
    if (SRC / "cal_sad_h2.png").exists():
        seq += ken_burns("cal_sad_h2", 3.0, "…and its owner is heartbroken")
    seq += smooth_clip("g_scrub", "Grab the Soapy Sponge and scrub", 2)
    if (SRC / "s4_waiting.png").exists():
        seq += ken_burns("s4_waiting", 2.5, "They wait by the door")
    seq += smooth_clip("g_celeb", "Home again!", 3, end=110)
    if (SRC / "cal_sad_h2.png").exists() and (SRC / "s6_clean_inside.png").exists():
        w = wipe_frames("cal_sad_h2", "s6_clean_inside", n=45, hold=30)
        seq += [x for x in w for _ in (0, 1)]
    if (SRC / "s8_portrait.png").exists():
        seq += ken_burns("s8_portrait", 2.5, "A thank-you letter, rewards and a family portrait")
    seq += [title_card("Fabric · NeoForge · Forge", "Minecraft 26.3")] * 90
    tmp = write_seq(seq, "t")
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-framerate", "30", "-i", str(tmp / "t%04d.png"),
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "20", "-pix_fmt", "yuv420p",
                    "-movflags", "+faststart", str(OUT / "trailer.mp4")], check=True)
    print("wrote trailer.mp4", (OUT / "trailer.mp4").stat().st_size // 1024, "KiB", len(seq) / 30, "s")
    shutil.rmtree(tmp)


if __name__ == "__main__":
    only = sys.argv[3] if len(sys.argv) > 3 else None
    if only == "trailer":
        trailer(); sys.exit()
    stills()
    for pre, tick, out, w, end, fps in (("g_scrub", 2, "scrubbing.gif", 440, 75, 15), ("g_celeb", 3, "celebration.gif", 640, 110, 20)):
        if frames(pre):
            d = smooth_frames(pre, tick, fps, w, 0, end)
            gif_from_dir(d, out, fps, w)
            shutil.rmtree(d)
    before_after_gif()
    if only != "notrailer":
        trailer()
