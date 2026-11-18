#!/usr/bin/env python3
"""
Downloads the compile-time libraries that build.py needs (Minecraft + loaders) into a folder.

Usage: python3 tools/fetch_libs.py [libs dir=libs]

Everything comes from the official sources: Mojang (game jar and libraries), FabricMC, NeoForged and
MinecraftForge maven repositories. Nothing is redistributed by this repository.
Versions can be overridden with environment variables (MC_VERSION, FABRIC_LOADER, NEOFORGE, FORGE).
"""
import json
import os
import re
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

DEST = Path(sys.argv[1] if len(sys.argv) > 1 else "libs")
MC = os.environ.get("MC_VERSION", "26.3")
FABRIC_LOADER = os.environ.get("FABRIC_LOADER", "0.19.3")
NEOFORGE = os.environ.get("NEOFORGE", "26.3.0.100")
FORGE = os.environ.get("FORGE", "26.3-66.1.8")

FABRIC_MAVEN = "https://maven.fabricmc.net/"
NEO_MAVEN = "https://maven.neoforged.net/releases/"
FORGE_MAVEN = "https://maven.minecraftforge.net/"
CENTRAL = "https://repo1.maven.org/maven2/"
MOJANG_LIBS = "https://libraries.minecraft.net/"
REPOS = [NEO_MAVEN, FORGE_MAVEN, MOJANG_LIBS, CENTRAL, FABRIC_MAVEN]
failures = []


def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "clean-village-build"})
    with urllib.request.urlopen(req, timeout=120) as r:
        return r.read()


def save(url, name=None, optional=False):
    name = name or url.rsplit("/", 1)[1]
    out = DEST / name
    if out.exists() and out.stat().st_size > 0:
        return True
    try:
        out.write_bytes(get(url))
        print("  ok  ", name)
        return True
    except (urllib.error.URLError, OSError) as e:
        print("  FAIL", url, e)
        if not optional:
            failures.append(url)
        return False


def maven_url(repo, group, artifact, version, classifier=None, ext="jar"):
    c = f"-{classifier}" if classifier else ""
    return f"{repo}{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}{c}.{ext}"


def fetch_coord(group, artifact, version, classifier=None, repos=REPOS):
    for repo in repos:
        if save(maven_url(repo, group, artifact, version, classifier), optional=True):
            return True
    failures.append(f"{group}:{artifact}:{version}")
    return False


def pom_dependencies(repo, group, artifact, version):
    """Direct compile/runtime dependencies listed in a POM (versions with ${...} are skipped)."""
    try:
        root = ET.fromstring(get(maven_url(repo, group, artifact, version, ext="pom")))
    except Exception as e:
        print("  (no pom for", artifact, version, ":", e, ")")
        return []
    ns = {"m": root.tag.split("}")[0].strip("{")}
    out = []
    for d in root.findall(".//m:dependencies/m:dependency", ns):
        g, a, v = (d.findtext(f"m:{k}", namespaces=ns) for k in ("groupId", "artifactId", "version"))
        scope = d.findtext("m:scope", namespaces=ns) or "compile"
        if g and a and v and not v.startswith("${") and scope in ("compile", "runtime"):
            out.append((g, a, v))
    return out


def mojang():
    print("== Minecraft", MC)
    manifest = json.loads(get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
    entry = next(v for v in manifest["versions"] if v["id"] == MC)
    meta = json.loads(get(entry["url"]))
    save(meta["downloads"]["client"]["url"], f"minecraft-{MC}.jar")
    for lib in meta["libraries"]:
        art = lib.get("downloads", {}).get("artifact")
        if art and "natives" not in art["path"]:
            save(art["url"], art["path"].rsplit("/", 1)[1])


def fabric():
    print("== Fabric")
    save(maven_url(FABRIC_MAVEN, "net.fabricmc", "fabric-loader", FABRIC_LOADER))
    fetch_coord("net.fabricmc", "sponge-mixin", "0.17.3+mixin.0.8.7", repos=[FABRIC_MAVEN])
    fetch_coord("net.fabricmc.fabric-api", "fabric-api-base", "2.0.4+ece063239e", repos=[FABRIC_MAVEN])
    fetch_coord("net.fabricmc.fabric-api", "fabric-creative-tab-api-v1", "5.0.15+6468574f9e", repos=[FABRIC_MAVEN])


def neoforge():
    print("== NeoForge", NEOFORGE)
    save(maven_url(NEO_MAVEN, "net.neoforged", "neoforge", NEOFORGE, "universal"))
    for g, a, v in pom_dependencies(NEO_MAVEN, "net.neoforged", "neoforge", NEOFORGE):
        if a in ("loader", "bus", "neoforgespi", "coremods", "mergetool", "accesstransformers"):
            fetch_coord(g, a, v)


def forge():
    print("== Forge", FORGE)
    save(maven_url(FORGE_MAVEN, "net.minecraftforge", "forge", FORGE, "universal"))
    for a in ("fmlcore", "fmlloader", "javafmllanguage"):
        fetch_coord("net.minecraftforge", a, FORGE, repos=[FORGE_MAVEN])
    for g, a, v in pom_dependencies(FORGE_MAVEN, "net.minecraftforge", "forge", FORGE):
        if a in ("eventbus", "bus", "forgespi"):
            fetch_coord(g, a, v)


if __name__ == "__main__":
    DEST.mkdir(parents=True, exist_ok=True)
    mojang()
    fabric()
    neoforge()
    forge()
    if failures:
        print("\nMissing:", *failures, sep="\n  ")
        sys.exit("some libraries could not be downloaded")
    print("\nAll libraries ready in", DEST)
