#!/usr/bin/env python3
"""
Headless test harness: starts a Fabric dedicated server (Minecraft 26.3) with Clean Village,
runs a list of console commands and prints the server log. Used by the validator agent.

Usage: python3 tools/harness.py [--fresh] [--wait SECONDS] "cmd1" "cmd2" ...
Each command can be "sleep N" to wait N seconds. The server is stopped at the end.
Environment (defaults match the dev container):
  CL_SERVER_DIR  /home/claude/fsrv          server directory (mods/ inside)
  CL_MC_JAR      /home/claude/mc26.3-j21.jar
  CL_LIBS        /home/claude/srvlibs
"""
import os
import queue
import shutil
import subprocess
import sys
import threading
import time
from pathlib import Path

SERVER = Path(os.environ.get("CL_SERVER_DIR", "/home/claude/fsrv"))
MC = os.environ.get("CL_MC_JAR", "/home/claude/mc26.3-j21.jar")
LIBS = Path(os.environ.get("CL_LIBS", "/home/claude/srvlibs"))
SHIM = os.environ.get("CL_SHIM", "/home/claude/j21shim.jar")
ROOT = Path(__file__).resolve().parent.parent


def classpath():
    jars = [str(p) for p in sorted(LIBS.glob("*.jar")) if "sponge-mixin-0.17.4" not in p.name]
    return os.pathsep.join([SHIM, MC] + jars)


def main():
    args = sys.argv[1:]
    fresh = "--fresh" in args
    args = [a for a in args if a != "--fresh"]
    if fresh:
        for d in ("world", "logs", "crash-reports"):
            shutil.rmtree(SERVER / d, ignore_errors=True)
    built = ROOT / "build/libs"
    for old in (SERVER / "mods").glob("cleanvillage-*.jar"):
        old.unlink()
    for j in [x for x in built.glob("cleanvillage-fabric-*.jar") if "-dev" not in x.name]:
        shutil.copy(j, SERVER / "mods" / j.name)
    cmd = ["java", "-Xmx3G", "--enable-preview", f"-Dfabric.gameJarPath={MC}",
           "-Dfabric.debug.replaceVersion=java:25", "-cp", classpath(),
           "net.fabricmc.loader.impl.launch.knot.KnotServer", "--nogui"]
    env = dict(os.environ, JAVA_TOOL_OPTIONS="", _JAVA_OPTIONS="")
    p = subprocess.Popen(cmd, cwd=SERVER, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                         text=True, env=env, bufsize=1)
    q = queue.Queue()

    def reader():
        for line in p.stdout:
            q.put(line)
        q.put(None)

    threading.Thread(target=reader, daemon=True).start()
    out = []

    def pump(timeout):
        end = time.time() + timeout
        while time.time() < end:
            try:
                line = q.get(timeout=0.2)
            except queue.Empty:
                continue
            if line is None:
                return "EOF"
            if "Picked up" in line:
                continue
            out.append(line)
            sys.stdout.write(line)
            sys.stdout.flush()
            if "Done (" in line and "For help" in line:
                return "DONE"
        return "TIMEOUT"

    state = pump(300)
    if state != "DONE":
        print(f"### server did not start ({state})")
        p.kill()
        sys.exit(2)
    for c in args:
        if c.startswith("sleep "):
            pump(float(c.split()[1]))
            continue
        print(f">>> {c}")
        p.stdin.write(c + "\n")
        p.stdin.flush()
        pump(1.5)
    p.stdin.write("stop\n")
    p.stdin.flush()
    pump(60)
    p.wait(timeout=30)
    errors = [l for l in out if "ERROR" in l or "Exception" in l]
    print(f"### finished, {len(errors)} error lines")


if __name__ == "__main__":
    main()
