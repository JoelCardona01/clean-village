package dev.cleanvillage.dev;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cleanvillage.dev.mixin.KeyMappingAccessor;
import dev.cleanvillage.dev.mixin.MinecraftInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * DEV ONLY (not shipped in release jars). Lets the test agent drive the client without keyboard focus:
 * it polls <gameDir>/cleanvillage-dev/script.txt, runs one line per tick and appends results to log.txt.
 * Lines: wait N | cmd <command> | chat <text> | screenshot <name> | hud on|off | use N | attack N |
 *        look <yaw> <pitch> | slot N | close | key <keymapping name> N | fov N | gamma X | clearchat |
 *        burst <name> <count> <everyTicks> | pan <yaw0> <pitch0> <yaw1> <pitch1> <ticks>
 */
public final class DevScript {
    private static final Deque<String> QUEUE = new ArrayDeque<>();
    private static int waitTicks;
    private static int useTicks;
    private static int attackTicks;
    private static int keyTicks;
    private static net.minecraft.client.KeyMapping heldKey;
    private static int poll;
    private static String burstName;
    private static int burstLeft, burstEvery, burstTimer, burstIndex;
    private static float panY0, panP0, panY1, panP1;
    private static int panTicks, panTotal;

    private DevScript() {}

    private static Path dir(Minecraft mc) {
        return mc.gameDirectory.toPath().resolve("cleanvillage-dev");
    }

    private static void log(Minecraft mc, String s) {
        try {
            Files.writeString(dir(mc).resolve("log.txt"), LocalTime.now() + " " + s + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    public static void tick(Minecraft mc) {
        Path d = dir(mc);
        if (++poll % 10 == 0 && Files.isDirectory(d)) {
            Path script = d.resolve("script.txt");
            if (Files.exists(script)) {
                try {
                    List<String> lines = Files.readAllLines(script, StandardCharsets.UTF_8);
                    Files.move(script, d.resolve("script.done"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    for (String l : lines) if (!l.isBlank() && !l.startsWith("#")) QUEUE.add(l.trim());
                    log(mc, "loaded " + lines.size() + " lines");
                } catch (IOException e) {
                    log(mc, "read failed " + e);
                }
            }
        }
        if (useTicks > 0 && --useTicks == 0) mc.options.keyUse.setDown(false);
        else if (useTicks > 0) mc.options.keyUse.setDown(true);
        if (attackTicks > 0 && --attackTicks == 0) mc.options.keyAttack.setDown(false);
        if (keyTicks > 0 && --keyTicks == 0 && heldKey != null) heldKey.setDown(false);
        if (panTicks > 0 && mc.player != null) {
            panTicks--;
            float t = 1f - panTicks / (float) panTotal;
            t = t * t * (3 - 2 * t);
            float y = panY0 + (panY1 - panY0) * t, pp = panP0 + (panP1 - panP0) * t;
            mc.player.setYRot(y); mc.player.setYHeadRot(y); mc.player.setXRot(pp);
            mc.player.yRotO = y; mc.player.xRotO = pp;
        }
        if (burstLeft > 0 && --burstTimer <= 0) {
            burstTimer = burstEvery;
            burstLeft--;
            String n = String.format("%s_%03d.png", burstName, burstIndex++);
            Screenshot.grab(mc.gameDirectory, n, mc.gameRenderer.mainRenderTarget(), 1, c -> {});
            if (burstLeft == 0) log(mc, "burst done " + burstName + " x" + burstIndex);
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        String line = QUEUE.poll();
        if (line == null) return;
        try {
            run(mc, line);
        } catch (Exception e) {
            log(mc, "FAILED " + line + " -> " + e);
        }
    }

    private static void run(Minecraft mc, String line) {
        String[] p = line.split(" ", 2);
        String arg = p.length > 1 ? p[1] : "";
        switch (p[0]) {
            case "wait" -> waitTicks = Integer.parseInt(arg.trim());
            case "cmd" -> {
                if (mc.player != null) mc.player.connection.sendCommand(arg.startsWith("/") ? arg.substring(1) : arg);
            }
            case "chat" -> {
                if (mc.player != null) mc.player.connection.sendChat(arg);
            }
            case "screenshot" -> Screenshot.grab(mc.gameDirectory, arg.trim() + ".png", mc.gameRenderer.mainRenderTarget(), 1,
                    c -> log(mc, "screenshot " + arg + ": " + c.getString()));
            case "hud" -> {
                ((KeyMappingAccessor) mc.options.keyToggleGui).cleanvillage$setClickCount(1);
            }
            case "use" -> {
                useTicks = Integer.parseInt(arg.trim());
                mc.options.keyUse.setDown(true);
                ((MinecraftInvoker) mc).cleanvillage$startUseItem();
            }
            case "attack" -> {
                attackTicks = Integer.parseInt(arg.trim());
                ((KeyMappingAccessor) mc.options.keyAttack).cleanvillage$setClickCount(1);
                mc.options.keyAttack.setDown(true);
            }
            case "key" -> {
                String[] k = arg.split(" ");
                heldKey = net.minecraft.client.KeyMapping.get(k[0]);
                if (heldKey != null) {
                    keyTicks = Integer.parseInt(k[1]);
                    ((KeyMappingAccessor) heldKey).cleanvillage$setClickCount(1);
                    heldKey.setDown(true);
                }
            }
            case "look" -> {
                if (mc.player != null) {
                    String[] a = arg.split(" ");
                    mc.player.setYRot(Float.parseFloat(a[0]));
                    mc.player.setYHeadRot(Float.parseFloat(a[0]));
                    mc.player.setXRot(Float.parseFloat(a[1]));
                }
            }
            case "lookat" -> {
                if (mc.player != null) {
                    String[] a = arg.split(" ");
                    double dx = Double.parseDouble(a[0]) - mc.player.getX();
                    double dy = Double.parseDouble(a[1]) - mc.player.getEyeY();
                    double dz = Double.parseDouble(a[2]) - mc.player.getZ();
                    float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                    float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
                    mc.player.setYRot(yaw);
                    mc.player.setYHeadRot(yaw);
                    mc.player.setXRot(pitch);
                }
            }
            case "fov" -> mc.options.fov().set(Integer.parseInt(arg.trim()));
            case "gamma" -> mc.options.gamma().set(Double.parseDouble(arg.trim()));
            case "clearchat" -> mc.gui.hud.getChat().clearMessages(false);
            case "burst" -> {
                String[] a = arg.split(" ");
                burstName = a[0]; burstLeft = Integer.parseInt(a[1]); burstEvery = Integer.parseInt(a[2]);
                burstTimer = 0; burstIndex = 0;
            }
            case "pan" -> {
                String[] a = arg.split(" ");
                panY0 = Float.parseFloat(a[0]); panP0 = Float.parseFloat(a[1]);
                panY1 = Float.parseFloat(a[2]); panP1 = Float.parseFloat(a[3]);
                panTotal = panTicks = Integer.parseInt(a[4]);
            }
            case "slot" -> {
                if (mc.player != null) mc.player.getInventory().setSelectedSlot(Integer.parseInt(arg.trim()));
            }
            case "openworld" -> mc.createWorldOpenFlows().openWorld(arg.trim(), () -> log(mc, "openworld failed"));
            case "quit" -> {
                if (mc.level != null) mc.disconnectWithSavingScreen();
            }
            case "exit" -> mc.stop();
            case "pos" -> {
                if (mc.player != null) log(mc, "pos " + mc.player.position() + " yaw " + mc.player.getYRot() + " pitch " + mc.player.getXRot()
                        + " hit=" + mc.hitResult);
            }
            default -> log(mc, "unknown " + line);
        }
        log(mc, "ok " + line);
    }

    static InputConstants.Key unused() {
        return null;
    }
}
