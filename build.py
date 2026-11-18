#!/usr/bin/env python3
"""
Offline build for Clean Village (common + Fabric + NeoForge + Forge).

Minecraft 26.x is not obfuscated, so the mod compiles directly against the game jar
with no remapping. Put the compile jars in a folder (default ./libs, or $CLEANVILLAGE_LIBS):
  minecraft-26.3.jar, brigadier, datafixerupper, guava, gson, fastutil, slf4j-api, log4j-api,
  jspecify, joml, netty-*, authlib, logging, commons-lang3, sponge-mixin, fabric-loader,
  fabric-api-base, fabric-creative-tab-api-v1, neoforge-*-universal, loader-*, bus-*,
  forge-26.3-*-universal, eventbus-7*, fmlcore, fmlloader, javafmllanguage, forgespi.

Usage:  python3 build.py            -> build/libs/cleanvillage-{fabric,neoforge,forge}-<version>.jar
"""
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
VERSION = "1.0.0+mc26.3"
LIBS = Path(os.environ.get("CLEANVILLAGE_LIBS", os.environ.get("CASALIMPIA_LIBS", ROOT / "libs")))
BUILD = ROOT / "build"
JAVAC = os.environ.get("JAVAC", "javac")
RELEASE = os.environ.get("JAVA_RELEASE", "21")


def jars(*prefixes):
    out = []
    for p in sorted(LIBS.glob("*.jar")):
        if not prefixes or any(p.name.startswith(x) for x in prefixes):
            out.append(str(p))
    return out


def compile_module(name, sources, classpath, out):
    files = [str(p) for p in sorted(Path(sources).rglob("*.java"))]
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)
    cmd = [JAVAC, "--release", RELEASE, "-encoding", "UTF-8", "-proc:none", "-nowarn", "-Xlint:-options",
           "-d", str(out), "-cp", os.pathsep.join(classpath)] + files
    print(f"[{name}] compiling {len(files)} files")
    env = dict(os.environ, JAVA_TOOL_OPTIONS="", _JAVA_OPTIONS="")
    r = subprocess.run(cmd, env=env)
    if r.returncode != 0:
        sys.exit(f"[{name}] compilation failed")


def add_tree(z, base, prefix=""):
    for p in sorted(Path(base).rglob("*")):
        if p.is_file():
            arc = prefix + p.relative_to(base).as_posix()
            data = p.read_bytes()
            if p.suffix in (".json", ".toml", ".mcmeta") and b"${version}" in data:
                data = data.replace(b"${version}", VERSION.encode())
            z.writestr(zipfile.ZipInfo(arc, date_time=(2026, 11, 18, 0, 0, 0)), data, zipfile.ZIP_DEFLATED)


DEV = "--dev" in sys.argv


def package(loader, classes):
    out = BUILD / "libs" / (f"cleanvillage-{loader}-{VERSION}" + ("-dev" if DEV else "") + ".jar")
    out.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(out, "w") as z:
        mixins = "cleanvillage.mixins.json" + (",cleanvillage-dev.mixins.json" if DEV and loader == "forge" else "")
        z.writestr("META-INF/MANIFEST.MF",
                   "Manifest-Version: 1.0\nImplementation-Title: cleanvillage\n"
                   f"Implementation-Version: {VERSION}\nMixinConfigs: {mixins}\n")
        add_tree(z, BUILD / "classes" / "common")
        add_tree(z, ROOT / "common/src/main/resources")
        add_tree(z, classes)
        res = ROOT / f"{loader}/src/main/resources"
        for p in sorted(res.rglob("*")):
            if p.is_file():
                arc = p.relative_to(res).as_posix()
                data = p.read_bytes().replace(b"${version}", VERSION.encode())
                if DEV and arc == "fabric.mod.json":
                    data = data.replace(b'"mixins": ["cleanvillage.mixins.json"]', b'"mixins": ["cleanvillage.mixins.json", "cleanvillage-dev.mixins.json"]')
                if DEV and arc.endswith("neoforge.mods.toml"):
                    data = data.replace(b'config = "cleanvillage.mixins.json"', b'config = "cleanvillage.mixins.json"\n\n[[mixins]]\nconfig = "cleanvillage-dev.mixins.json"')
                z.writestr(zipfile.ZipInfo(arc, date_time=(2026, 11, 18, 0, 0, 0)), data, zipfile.ZIP_DEFLATED)
        if DEV:
            add_tree(z, BUILD / "classes" / "devtools")
            add_tree(z, ROOT / "devtools/src/main/resources")
        lic = ROOT / "LICENSE"
        if lic.exists():
            z.write(lic, "LICENSE")
    print(f"[{loader}] -> {out.relative_to(ROOT)} ({out.stat().st_size // 1024} KiB)")
    return out


def main():
    base = jars("minecraft", "brigadier", "datafixerupper", "guava", "failureaccess", "gson", "fastutil", "slf4j",
                "log4j-api", "jspecify", "joml", "netty", "authlib", "logging", "commons-lang3", "sponge-mixin")
    if not any("minecraft" in j for j in base):
        sys.exit(f"minecraft-26.3.jar not found in {LIBS}")
    common_out = BUILD / "classes" / "common"
    compile_module("common", ROOT / "common/src/main/java", base, common_out)
    fabric_out = BUILD / "classes" / "fabric"
    compile_module("fabric", ROOT / "fabric/src/main/java",
                   base + [str(common_out)] + jars("fabric-loader", "fabric-api-base", "fabric-creative-tab"), fabric_out)
    neo_out = BUILD / "classes" / "neoforge"
    compile_module("neoforge", ROOT / "neoforge/src/main/java",
                   base + [str(common_out)] + jars("neoforge", "loader-", "bus-"), neo_out)
    forge_out = BUILD / "classes" / "forge"
    compile_module("forge", ROOT / "forge/src/main/java",
                   base + [str(common_out)] + jars("forge-26", "eventbus-", "fmlcore", "fmlloader", "javafmllanguage", "forgespi"), forge_out)
    if DEV:
        compile_module("devtools", ROOT / "devtools/src/main/java", base + [str(common_out)], BUILD / "classes" / "devtools")
    package("fabric", fabric_out)
    package("neoforge", neo_out)
    package("forge", forge_out)


if __name__ == "__main__":
    main()
