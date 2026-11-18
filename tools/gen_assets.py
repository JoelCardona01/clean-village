#!/usr/bin/env python3
"""
Generates every asset and data file of Spotless Villages:
 - 16x16 textures (pixel art made procedurally + derived from vanilla textures)
 - blockstates / block models / item models
 - en_us + es_es (+ es_mx, es_ar) lang files
 - sounds.json, recipes and the reward loot table

Usage: python3 tools/gen_assets.py <extracted vanilla jar dir> [es_es.json]
The vanilla dir must contain assets/minecraft/... (unzipped client jar).
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "common/src/main/resources"
A = RES / "assets/spotless"
D = RES / "data/spotless"
VAN = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("/tmp/mcx")
VLANG = Path(sys.argv[2]) if len(sys.argv) > 2 else None  # folder with vanilla <lang>.json files
VT = VAN / "assets/minecraft/textures"
VM = VAN / "assets/minecraft/models/block"

sys.path.insert(0, str(Path(__file__).parent))
import content  # noqa: E402  (texts: stories, letters, phrases)

CUBES = ["cobblestone", "mossy_cobblestone", "stone_bricks", "oak_planks", "spruce_planks", "birch_planks",
         "acacia_planks", "jungle_planks", "dark_oak_planks", "smooth_sandstone", "cut_sandstone", "sandstone",
         "terracotta", "white_terracotta", "yellow_terracotta", "packed_ice", "snow_block", "bricks"]
PILLARS = ["oak_log", "spruce_log", "birch_log", "acacia_log", "jungle_log", "dark_oak_log",
           "stripped_oak_log", "stripped_spruce_log", "stripped_acacia_log", "stripped_birch_log",
           "oak_wood", "spruce_wood", "acacia_wood", "stripped_oak_wood", "stripped_spruce_wood", "stripped_acacia_wood"]


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def save(img, rel):
    p = A / "textures" / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def vtex(rel):
    return Image.open(VT / rel).convert("RGBA")


# ---------------------------------------------------------------- noise helpers
def value_noise(seed, size=16, cell=4):
    rnd = random.Random(seed)
    g = [[rnd.random() for _ in range(size // cell + 2)] for _ in range(size // cell + 2)]

    def f(x, y):
        gx, gy = x / cell, y / cell
        x0, y0 = int(gx), int(gy)
        tx, ty = gx - x0, gy - y0
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = g[y0][x0] * (1 - tx) + g[y0][x0 + 1] * tx
        b = g[y0 + 1][x0] * (1 - tx) + g[y0 + 1][x0 + 1] * tx
        return a * (1 - ty) + b * ty
    return f


def blotches(seed, coverage, palette, speck=0.08):
    """Transparent 16x16 with irregular blotches covering ~coverage of the tile."""
    n1, n2 = value_noise(seed, cell=4), value_noise(seed + 7, cell=2)
    rnd = random.Random(seed * 3 + 1)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    vals = sorted(0.65 * n1(x, y) + 0.35 * n2(x, y) for y in range(16) for x in range(16))
    thr = vals[int((1 - coverage) * 255)]
    for y in range(16):
        for x in range(16):
            v = 0.65 * n1(x, y) + 0.35 * n2(x, y)
            if v >= thr or rnd.random() < speck * coverage:
                t = min(1.0, (v - thr) / 0.25) if v >= thr else 0.0
                col = palette[min(len(palette) - 1, int(t * len(palette)))]
                img.putpixel((x, y), col)
    return img


# ---------------------------------------------------------------- textures
def gen_textures():
    grime_pal = [hexc("#6b5136"), hexc("#574129"), hexc("#46331f"), hexc("#3a2a1a")]
    for lvl, cov in ((1, 0.22), (2, 0.42), (3, 0.66)):
        save(blotches(100 + lvl, cov, grime_pal, speck=0.25), f"block/grime_{lvl}.png")
    mold_pal = [hexc("#7a9a3a"), hexc("#5b7f2c"), hexc("#456a24"), hexc("#30501c")]
    for lvl, cov in ((1, 0.30), (2, 0.55)):
        img = blotches(200 + lvl, cov, mold_pal, speck=0.2)
        rnd = random.Random(lvl)
        for _ in range(4 + lvl * 2):  # little fuzzy light spores
            x, y = rnd.randrange(16), rnd.randrange(16)
            if img.getpixel((x, y))[3]:
                img.putpixel((x, y), hexc("#a9c45a"))
        save(img, f"block/mold_{lvl}.png")

    # overlays for dirty full blocks: stains + vertical drips
    stain_pal = [hexc("#5f4a33", 255), hexc("#4c3a27", 255), hexc("#3d2e1f", 255)]
    for stage, cov in ((1, 0.18), (2, 0.38)):
        img = blotches(300 + stage, cov, stain_pal, speck=0.3)
        rnd = random.Random(stage * 11)
        for _ in range(2 + stage * 2):
            x = rnd.randrange(16)
            y0 = rnd.randrange(0, 6)
            for y in range(y0, min(16, y0 + rnd.randrange(3, 9))):
                img.putpixel((x, y), hexc("#4a3926"))
        save(img, f"block/dirt_overlay_{stage}.png")

    # fogged glass (translucent)
    for src, dst in (("block/glass.png", "block/dirty_glass.png"), ("block/glass_pane_top.png", "block/dirty_glass_pane_top.png")):
        g = vtex(src)
        n = value_noise(400, cell=4)
        out = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                r, gg, b, a = g.getpixel((x, y))
                v = n(x, y)
                fog = (150 + int(40 * v), 138 + int(30 * v), 112 + int(20 * v))
                if a > 0 and a > 200:  # frame pixels: keep, darker
                    out.putpixel((x, y), (int(r * 0.7), int(gg * 0.68), int(b * 0.6), 255))
                else:
                    alpha = 120 + int(90 * v)
                    out.putpixel((x, y), (*fog, alpha))
        # smudges
        rnd = random.Random(7)
        for _ in range(14):
            x, y = rnd.randrange(1, 15), rnd.randrange(1, 15)
            out.putpixel((x, y), (92, 74, 52, 215))
        save(out, dst)

    # dusty cobweb: procedural web (spokes + rings) in dusty grey, with dust specks
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rnd = random.Random(9)
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            ang = (math.degrees(math.atan2(dy, dx)) + 360) % 45
            spoke = min(ang, 45 - ang) * r * math.pi / 180 < 0.32
            ring = any(abs(r * (0.92 + 0.08 * math.cos(math.radians(ang * 8))) - rr) < 0.32 for rr in (2.6, 5.0, 7.3))
            if (spoke or ring) and r < 8.2:
                c = (214, 208, 196, 255) if rnd.random() > 0.18 else (150, 132, 108, 255)
                out.putpixel((x, y), c)
            elif rnd.random() < 0.03:
                out.putpixel((x, y), (128, 112, 92, 255))
    save(out, "block/dusty_cobweb.png")

    save(portrait(), "block/family_portrait.png")
    save(portrait_side(), "block/family_portrait_frame.png")
    save(sponge_item(), "item/soapy_sponge.png")


def outline(img, color):
    out = img.copy()
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and img.getpixel((nx, ny))[3] > 0 and img.getpixel((nx, ny)) != color:
                        out.putpixel((x, y), color)
                        break
    return out


def diag_tool(kind):
    """Tools drawn along the 45 degree diagonal like vanilla tools (handle bottom-left)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wood, wood_l, wood_d = hexc("#9c6b3a"), hexc("#c08a4f"), hexc("#6b4524")
    rnd = random.Random(kind)
    for y in range(16):
        for x in range(16):
            u, v = x - y, x + y   # u along the tool, v across
            if kind == "brush":
                if v == 15 and -11 <= u <= -3:
                    img.putpixel((x, y), wood_l if u % 4 == 0 else wood)
                elif 12 <= v <= 18 and -2 <= u <= 10:
                    if v <= 14:
                        img.putpixel((x, y), wood_l if v == 12 else wood if v == 13 else wood_d)
                    else:
                        c = [hexc("#4b8fd6"), hexc("#3a74b8"), hexc("#2c5a91"), hexc("#86bdf0")][v - 15]
                        if v == 18 and u % 2:  # ragged bristle tips
                            continue
                        img.putpixel((x, y), c)
            else:  # feather duster
                if v == 15 and -11 <= u <= -1:
                    img.putpixel((x, y), wood_l if u % 4 == 0 else wood)
                if v in (15, 16) and u in (-1, 0):
                    img.putpixel((x, y), hexc("#e0b43a"))
                du, dv = (u - 6) / 6.2, (v - 15) / 4.8
                if du * du + dv * dv <= 1.0 and u >= 0:
                    t = rnd.random()
                    c = hexc("#fbf3f6") if t < 0.45 else hexc("#f2b6c9") if t < 0.75 else hexc("#d97b9c") if t < 0.92 else hexc("#ffffff")
                    img.putpixel((x, y), c)
    img = outline(img, hexc("#2b1d12") if kind == "brush" else hexc("#5c3346"))
    return img


def sponge_item():
    sp = vtex("block/sponge.png")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    x0, y0, x1, y1 = 2, 6, 13, 14
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            corner = (x in (x0, x1)) and (y in (y0, y1))
            if corner:
                continue
            r, g, b, a = sp.getpixel((x, y))
            k = 1.0
            if x == x1 or y == y1:
                k = 0.72
            elif x == x0 or y == y0:
                k = 1.08
            img.putpixel((x, y), (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)), 255))
    foam = [(3, 6), (4, 5), (5, 5), (6, 5), (7, 4), (8, 4), (9, 5), (10, 5), (11, 6), (12, 6), (4, 6), (5, 6), (6, 6),
            (7, 5), (8, 5), (9, 6), (10, 6), (6, 4), (8, 3), (9, 4), (3, 7), (7, 6), (8, 6), (11, 7)]
    for (x, y) in foam:
        img.putpixel((x, y), hexc("#ffffff") if (x + y) % 3 else hexc("#d6ecff"))
    # bubbles
    for (bx, by) in ((12, 2), (3, 2), (14, 4)):
        for (dx, dy) in ((0, -1), (1, 0), (0, 1), (-1, 0)):
            x, y = bx + dx, by + dy
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), hexc("#bfe3ff"))
        img.putpixel((bx - 1, by - 1) if bx > 0 and by > 0 else (bx, by), hexc("#ffffff"))
    return outline(img, hexc("#7a6420"))


def soap_bucket():
    img = vtex("item/water_bucket.png")
    rnd = random.Random(5)
    for y in range(16):
        for x in range(16):
            r, g, b, a = img.getpixel((x, y))
            if a and b > r + 40:
                img.putpixel((x, y), hexc("#ffffff") if rnd.random() < 0.55 else hexc("#cfe9ff"))
    for (x, y, c) in ((6, 0, "#e8f6ff"), (7, 0, "#ffffff"), (9, 0, "#cfe9ff"), (11, 1, "#ffffff"), (4, 1, "#e8f6ff"),
                      (13, 0, "#bfe3ff"), (2, 0, "#bfe3ff")):
        img.putpixel((x, y), hexc(c))
    return img


def portrait():
    """16x16 front: golden frame, two villagers (parent + child) on a sky background."""
    W = hexc("#7b4f22")
    G = hexc("#d8a845")
    g = hexc("#a9792b")
    rows = [
        "WWWWWWWWWWWWWWWW",
        "WGGGGGGGGGGGGGGW",
        "WGsssssssssssgGW",
        "WGsssssssssssgGW",
        "WGssbbbsssssssGW",
        "WGssbbbssskkksGW",
        "WGsbbnbbsskkksGW",
        "WGssbbbssskkksGW",
        "WGssbnbsssknkkGW",
        "WGsrrrrrsrrrrsGW",
        "WGrrrrrrrrrrrrGW",
        "WGhhhhhhhhhhhhGW",
        "WGhhhhhhhhhhhhGW",
        "WGggggggggggggGW",
        "WGGGGGGGGGGGGGGW",
        "WWWWWWWWWWWWWWWW",
    ]
    pal = {"W": W, "G": G, "g": g, "s": hexc("#8fc4ef"), "b": hexc("#c58f6a"), "k": hexc("#d9a07a"),
           "n": hexc("#a36a49"), "r": hexc("#6a4a8c"), "h": hexc("#5c9b3c")}
    img = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            img.putpixel((x, y), pal[ch])
    # eyes
    for (x, y) in ((4, 5), (6, 5), (10, 6), (12, 6)):
        img.putpixel((x, y), hexc("#2e5d2e"))
    return img


def portrait_side():
    img = Image.new("RGBA", (16, 16), hexc("#7b4f22"))
    for x in range(16):
        img.putpixel((x, 0), hexc("#d8a845"))
        img.putpixel((x, 15), hexc("#5a3815"))
    return img


# ---------------------------------------------------------------- models / blockstates
def vanilla_block_textures(name):
    """Return per-face texture refs of a vanilla full block model."""
    m = json.loads((VM / f"{name}.json").read_text())
    t = m.get("textures", {})
    parent = m.get("parent", "")
    tex = lambda k: t[k] if ":" in t[k] else "minecraft:" + t[k]
    if parent.endswith("cube_all"):
        a = tex("all")
        return dict(down=a, up=a, north=a, south=a, west=a, east=a, particle=a)
    if parent.endswith("cube_column"):
        e, s = tex("end"), tex("side")
        return dict(down=e, up=e, north=s, south=s, west=s, east=s, particle=s)
    if parent.endswith("cube_bottom_top"):
        b, u, s = tex("bottom"), tex("top"), tex("side")
        return dict(down=b, up=u, north=s, south=s, west=s, east=s, particle=s)
    raise SystemExit(f"Unsupported parent {parent} for {name}")


def gen_models():
    bm = A / "models/block"
    # parent with base cube + slightly inflated overlay cube
    faces = ["down", "up", "north", "south", "west", "east"]
    write_json(bm / "dirty_cube.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "#north"},
        "elements": [
            {"from": [0.02, 0.02, 0.02], "to": [15.98, 15.98, 15.98],
             "faces": {f: {"uv": [0, 0, 16, 16], "texture": "#" + f, "cullface": f} for f in faces}},
            {"from": [0, 0, 0], "to": [16, 16, 16],
             "faces": {f: {"uv": [0, 0, 16, 16], "texture": "#overlay", "cullface": f} for f in faces}},
        ],
    })
    for name in CUBES + PILLARS:
        t = vanilla_block_textures(name)
        pillar = name in PILLARS
        for stage in (1, 2):
            tx = dict(t)
            tx.pop("particle")
            tx["particle"] = t["particle"]
            tx["overlay"] = f"spotless:block/dirt_overlay_{stage}"
            write_json(bm / f"dirty_{name}_{stage}.json", {"parent": "spotless:block/dirty_cube", "textures": tx})
        if pillar:
            variants = {}
            for stage in (1, 2):
                m = f"spotless:block/dirty_{name}_{stage}"
                variants[f"axis=x,stage={stage}"] = {"model": m, "x": 90, "y": 90}
                variants[f"axis=y,stage={stage}"] = {"model": m}
                variants[f"axis=z,stage={stage}"] = {"model": m, "x": 90}
        else:
            variants = {f"stage={s}": {"model": f"spotless:block/dirty_{name}_{s}"} for s in (1, 2)}
        write_json(A / f"blockstates/dirty_{name}.json", {"variants": variants})

    # fogged glass pane
    pane_tex = {"edge": {"force_translucent": True, "sprite": "spotless:block/dirty_glass_pane_top"},
                "pane": {"force_translucent": True, "sprite": "spotless:block/dirty_glass"}}
    for part in ("post", "side", "side_alt", "noside", "noside_alt"):
        write_json(bm / f"dirty_glass_pane_{part}.json", {"parent": f"minecraft:block/template_glass_pane_{part}", "textures": pane_tex})
    gp = json.loads((VAN / "assets/minecraft/blockstates/glass_pane.json").read_text())
    for e in gp["multipart"]:
        e["apply"]["model"] = e["apply"]["model"].replace("minecraft:block/glass_pane", "spotless:block/dirty_glass_pane")
    write_json(A / "blockstates/dirty_glass_pane.json", gp)

    # grime & mold: multiface, one model per level, glow-lichen layout
    lichen = json.loads((VAN / "assets/minecraft/blockstates/glow_lichen.json").read_text())
    for block, levels, prefix in (("grime", (1, 2, 3), "grime"), ("mold", (1, 2), "mold")):
        parts = []
        for lvl in levels:
            write_json(bm / f"{prefix}_{lvl}.json", {
                "ambientocclusion": False,
                "textures": {"particle": f"spotless:block/{prefix}_{lvl}", "layer": f"spotless:block/{prefix}_{lvl}"},
                "elements": [{"from": [0, 0, 0.1], "to": [16, 16, 0.1],
                              "faces": {"north": {"uv": [16, 0, 0, 16], "texture": "#layer"},
                                        "south": {"uv": [0, 0, 16, 16], "texture": "#layer"}}}]})
            for e in lichen["multipart"]:
                when = e.get("when", {})
                if not any(v == "true" for v in when.values()):
                    continue
                apply = dict(e["apply"])
                apply["model"] = f"spotless:block/{prefix}_{lvl}"
                w = dict(when)
                w["level"] = str(lvl)
                parts.append({"apply": apply, "when": w})
        write_json(A / f"blockstates/{block}.json", {"multipart": parts})

    write_json(bm / "dusty_cobweb.json", {"parent": "minecraft:block/cross", "textures": {"cross": "spotless:block/dusty_cobweb"}})
    write_json(A / "blockstates/dusty_cobweb.json", {"variants": {"": {"model": "spotless:block/dusty_cobweb"}}})

    # family portrait: thin framed box on the wall (facing = direction the picture looks at)
    write_json(bm / "family_portrait.json", {
        "textures": {"particle": "spotless:block/family_portrait_frame", "front": "spotless:block/family_portrait",
                     "frame": "spotless:block/family_portrait_frame"},
        "elements": [{"from": [2, 3, 15], "to": [14, 14, 16], "faces": {
            "north": {"uv": [0, 0, 16, 16], "texture": "#front"},
            "south": {"uv": [0, 0, 16, 16], "texture": "#frame", "cullface": "south"},
            "up": {"uv": [0, 0, 16, 1], "texture": "#frame"},
            "down": {"uv": [0, 15, 16, 16], "texture": "#frame"},
            "west": {"uv": [0, 0, 1, 16], "texture": "#frame"},
            "east": {"uv": [15, 0, 16, 16], "texture": "#frame"}}}]})
    write_json(A / "blockstates/family_portrait.json", {"variants": {
        "facing=north": {"model": "spotless:block/family_portrait"},
        "facing=east": {"model": "spotless:block/family_portrait", "y": 90},
        "facing=south": {"model": "spotless:block/family_portrait", "y": 180},
        "facing=west": {"model": "spotless:block/family_portrait", "y": 270}}})

    # item models
    im = A / "models/item"
    tool_display = {
        "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
        "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    }
    for it in ("soapy_sponge",):
        model = {"parent": "minecraft:item/handheld" if it in ("scrub_brush", "feather_duster") else "minecraft:item/generated",
                 "textures": {"layer0": f"spotless:item/{it}"}}
        if it == "soapy_sponge":
            model["display"] = tool_display
        write_json(im / f"{it}.json", model)
        write_json(A / f"items/{it}.json", {"model": {"type": "minecraft:model", "model": f"spotless:item/{it}"}})
    write_json(im / "family_portrait.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "spotless:block/family_portrait"}})
    write_json(A / "items/family_portrait.json", {"model": {"type": "minecraft:model", "model": "spotless:item/family_portrait"}})


# ---------------------------------------------------------------- sounds
def gen_sounds():
    write_json(A / "sounds.json", {
        "scrub": {"subtitle": "subtitles.spotless.scrub", "sounds": [
            {"name": "minecraft:item/brush/brushing_generic1", "pitch": 1.3, "volume": 0.8},
            {"name": "minecraft:item/brush/brushing_generic2", "pitch": 1.25, "volume": 0.8},
            {"name": "minecraft:item/brush/brushing_generic3", "pitch": 1.35, "volume": 0.8},
            {"name": "minecraft:block/sponge/step3", "pitch": 1.2, "volume": 0.6}]},
        "bubbles": {"subtitle": "subtitles.spotless.bubbles", "sounds": [
            {"name": "minecraft:block/bubble_column/bubble1", "pitch": 1.4, "volume": 0.6},
            {"name": "minecraft:block/bubble_column/bubble2", "pitch": 1.5, "volume": 0.6},
            {"name": "minecraft:block/bubble_column/bubble3", "pitch": 1.3, "volume": 0.6},
            {"name": "minecraft:ambient/underwater/additions/bubbles2", "pitch": 1.6, "volume": 0.5}]},
        "sparkle": {"subtitle": "subtitles.spotless.sparkle", "sounds": [
            {"name": "minecraft:block/amethyst/shimmer", "pitch": 1.4},
            {"name": "minecraft:block/amethyst/resonate1", "pitch": 1.6, "volume": 0.7},
            {"name": "minecraft:note/icechime", "pitch": 1.5, "volume": 0.5}]},
        "drip": {"subtitle": "subtitles.spotless.drip", "sounds": [
            {"name": "minecraft:block/pointed_dripstone/drip_water1", "volume": 0.6},
            {"name": "minecraft:block/pointed_dripstone/drip_water2", "volume": 0.6},
            {"name": "minecraft:block/pointed_dripstone/drip_water3", "volume": 0.6},
            {"name": "minecraft:block/pointed_dripstone/drip_water4", "volume": 0.6}]},
        "sponge_dry": {"subtitle": "subtitles.spotless.sponge_dry", "sounds": [
            {"name": "minecraft:block/sponge/absorb1", "pitch": 1.4},
            {"name": "minecraft:block/sponge/absorb2", "pitch": 1.4}]},
    })


# ---------------------------------------------------------------- data
def gen_data():
    rec = D / "recipe"
    write_json(rec / "soapy_sponge.json", {
        "type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": ["minecraft:yellow_wool", "minecraft:slime_ball"],
        "result": {"id": "spotless:soapy_sponge"}})
    write_json(D / "loot_table/gameplay/house_reward.json", {
        "type": "minecraft:gift",
        "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:emerald",
                                      "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 3, "max": 7}}]}]},
            {"rolls": 1, "entries": [
                {"type": "minecraft:item", "name": "minecraft:gold_ingot", "weight": 6,
                 "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 4}}]},
                {"type": "minecraft:item", "name": "minecraft:cake", "weight": 2},
                {"type": "minecraft:item", "name": "minecraft:cookie", "weight": 2,
                 "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 4, "max": 8}}]},
                {"type": "minecraft:empty", "weight": 2}]},
            {"rolls": 1, "entries": [
                {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 1},
                {"type": "minecraft:empty", "weight": 9}]},
        ]})


# ---------------------------------------------------------------- lang
def gen_lang():
    for lang in content.LANGS:
        vf = (VLANG / f"{lang}.json") if VLANG else None
        van = json.loads(vf.read_text(encoding="utf-8")) if vf and vf.exists() else \
            json.loads((VAN / "assets/minecraft/lang/en_us.json").read_text(encoding="utf-8"))
        L = dict(content.TEXTS[lang])
        for name in CUBES + PILLARS + ["glass_pane"]:
            L[f"block.spotless.dirty_{name}"] = content.dirty_name(lang, van.get(f"block.minecraft.{name}", name))
        write_json(A / f"lang/{lang}.json", L)
        if lang == "es_es":
            for variant in ("es_mx", "es_ar", "es_cl", "es_uy", "es_ve", "es_ec"):
                write_json(A / f"lang/{variant}.json", L)
        if lang == "fr_fr":
            write_json(A / "lang/fr_ca.json", L)
        if lang == "de_de":
            write_json(A / "lang/de_at.json", L)
            write_json(A / "lang/de_ch.json", L)


if __name__ == "__main__":
    gen_textures()
    gen_models()
    gen_sounds()
    gen_data()
    gen_lang()
    print("assets generated in", RES)
