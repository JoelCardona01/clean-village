package dev.cleanvillage.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.cleanvillage.block.Cleanable;
import dev.cleanvillage.clean.Cleaning;
import dev.cleanvillage.house.DirtGenerator;
import dev.cleanvillage.house.House;
import dev.cleanvillage.house.HouseData;
import dev.cleanvillage.house.HouseManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/** /cleanvillage ... debug commands for testing (Hito 0). Requires cheats (level 2). */
public final class DebugCommands {
    private DebugCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("cleanvillage")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("testhouse").executes(DebugCommands::testHouse))
                .then(Commands.literal("village").executes(DebugCommands::village))
                .then(Commands.literal("verify").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(DebugCommands::verify)))
                .then(Commands.literal("scrub").then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .then(Commands.argument("units", IntegerArgumentType.integer(1)).executes(DebugCommands::scrub))))
                .then(Commands.literal("here").executes(DebugCommands::here))
                .then(Commands.literal("list").executes(DebugCommands::list))
                .then(Commands.literal("view").then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .then(Commands.argument("what", StringArgumentType.word()).executes(DebugCommands::view))))
                .then(Commands.literal("where").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(DebugCommands::where)))
                .then(Commands.literal("info").executes(DebugCommands::info))
                .then(Commands.literal("tp").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(DebugCommands::tp)))
                .then(Commands.literal("phase").then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .then(Commands.argument("phase", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    for (House.Phase p : House.Phase.values()) b.suggest(p.name().toLowerCase(Locale.ROOT));
                                    return b.buildFuture();
                                })
                                .executes(DebugCommands::phase))))
                .then(Commands.literal("clean").then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .then(Commands.argument("units", IntegerArgumentType.integer(1)).executes(DebugCommands::clean))))
                .then(Commands.literal("complete").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(DebugCommands::complete)))
                .then(Commands.literal("remove").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(DebugCommands::remove)))
        );
    }

    /** Prints the key positions of a house (for scripted screenshots). */
    private static int where(CommandContext<CommandSourceStack> c) {
        House h = house(c);
        if (h == null) return fail(c.getSource(), "No such house");
        ok(c.getSource(), "WHERE " + h.id + " bed=" + h.bed.toShortString() + " seat=" + h.seat.toShortString() + " yaw=" + h.seatYaw
                + " wait=" + h.waitPos.toShortString() + " inside=" + h.insidePos.toShortString() + " chest=" + h.chest.toShortString()
                + " box=" + h.box.minX() + "," + h.box.minY() + "," + h.box.minZ() + " -> " + h.box.maxX() + "," + h.box.maxY() + "," + h.box.maxZ());
        return 1;
    }

    /** Teleport the player to a nice viewpoint of a house, looking at it (for screenshots and tests). */
    private static int view(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        House h = house(c);
        if (h == null) return fail(c.getSource(), "No such house");
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel level = c.getSource().getLevel();
        String what = StringArgumentType.getString(c, "what");
        Vec3 from, to;
        Vec3 seat = Vec3.atBottomCenterOf(h.seat).add(0, 1.2, 0);
        Direction face = Direction.fromYRot(h.seatYaw);
        switch (what) {
            case "inside" -> {
                from = Vec3.atBottomCenterOf(h.insidePos);
                to = seat;
            }
            case "villager" -> {
                BlockPos f = h.seat.relative(face, 2);
                from = DirtGenerator.standable(level, f) ? Vec3.atBottomCenterOf(f) : Vec3.atBottomCenterOf(h.seat.relative(face));
                to = seat;
            }
            case "celebrate" -> {
                Vec3 target = Vec3.atBottomCenterOf(h.insidePos);
                BlockPos best = h.insidePos;
                double bestD = -1;
                for (BlockPos q : DirtGenerator.interiorCells(level, h.box)) {
                    if (!DirtGenerator.standable(level, q) || Math.abs(q.getY() - h.insidePos.getY()) > 0) continue;
                    double d = q.distSqr(h.insidePos);
                    if (d > bestD) {
                        bestD = d;
                        best = q.immutable();
                    }
                }
                from = Vec3.atBottomCenterOf(best);
                to = target.add(0, 1.0, 0);
            }
            case "chest" -> {
                from = Vec3.atBottomCenterOf(h.insidePos);
                to = Vec3.atCenterOf(h.chest);
            }
            default -> {
                Vec3 center = Vec3.atCenterOf(h.box.getCenter());
                Vec3 w = Vec3.atBottomCenterOf(h.waitPos);
                Vec3 dir = new Vec3(w.x - center.x, 0, w.z - center.z).normalize();
                from = w.add(dir.scale(5)).add(0, 2, 0);
                to = center;
            }
        }
        Vec3 eye = from.add(0, p.getEyeHeight(), 0);
        double dx = to.x - eye.x, dy = to.y - eye.y, dz = to.z - eye.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        p.teleportTo(level, from.x, from.y, from.z, java.util.Set.of(), yaw, pitch, false);
        ok(c.getSource(), "VIEW " + what + " of house " + h.id);
        return 1;
    }

    private static int fail(CommandSourceStack src, String msg) {
        src.sendFailure(Component.literal(msg));
        return 0;
    }

    private static void ok(CommandSourceStack src, String msg) {
        src.sendSuccess(() -> Component.literal(msg), true);
    }

    private static House house(CommandContext<CommandSourceStack> c) {
        return HouseData.get(c.getSource().getLevel()).byId(IntegerArgumentType.getInteger(c, "id"));
    }

    private static String describe(House h) {
        return "#" + h.id + " " + h.ownerName + " [" + h.phase + "] " + h.percent() + "% (" + (h.total - h.remaining) + "/" + h.total
                + ") at " + h.bed.toShortString() + " helpers=" + h.helpers.size();
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        HouseData data = HouseData.get(c.getSource().getLevel());
        if (data.houses().isEmpty()) ok(c.getSource(), "No dirty houses yet.");
        for (House h : data.houses()) ok(c.getSource(), describe(h));
        return data.houses().size();
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        House h = HouseManager.houseAt(level, BlockPos.containing(c.getSource().getPosition()));
        if (h == null) return fail(c.getSource(), "You are not inside an active dirty house.");
        ok(c.getSource(), describe(h) + " box=" + h.box);
        return 1;
    }

    private static int tp(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        House h = house(c);
        if (h == null) return fail(c.getSource(), "No such house");
        ServerPlayer p = c.getSource().getPlayerOrException();
        Vec3 at = Vec3.atBottomCenterOf(h.waitPos);
        p.teleportTo(c.getSource().getLevel(), at.x, at.y, at.z, java.util.Set.of(), p.getYRot(), p.getXRot(), false);
        ok(c.getSource(), "Teleported to " + describe(h));
        return 1;
    }

    private static int phase(CommandContext<CommandSourceStack> c) {
        House h = house(c);
        if (h == null) return fail(c.getSource(), "No such house");
        House.Phase phase;
        try {
            phase = House.Phase.valueOf(StringArgumentType.getString(c, "phase").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fail(c.getSource(), "Unknown phase");
        }
        HouseManager.forcePhase(c.getSource().getLevel(), h, phase);
        ok(c.getSource(), "House " + h.id + " -> " + phase);
        return 1;
    }

    private static int clean(CommandContext<CommandSourceStack> c) {
        House h = house(c);
        if (h == null) return fail(c.getSource(), "No such house");
        int units = IntegerArgumentType.getInteger(c, "units");
        ServerPlayer p = c.getSource().getPlayer();
        HouseManager.onCleaned(c.getSource().getLevel(), h.bed, p, units);
        ok(c.getSource(), describe(h));
        return 1;
    }

    /** Removes every dirt block in the house (as if cleaned by the executor) and triggers the celebration. */
    private static int complete(CommandContext<CommandSourceStack> c) {
        House h = house(c);
        if (h == null) return fail(c.getSource(), "No such house");
        ServerLevel level = c.getSource().getLevel();
        BoundingBox b = h.box.inflatedBy(1);
        Cleaning.suppressLoss(() -> {
            for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
                BlockState s = level.getBlockState(p);
                int guard = 0;
                while (s.getBlock() instanceof Cleanable cl && guard++ < 5) {
                    s = cl.cleanedOnce(s);
                }
                if (guard > 0) level.setBlock(p, s, Block.UPDATE_ALL);
            }
        });
        if (h.remaining > 0) HouseManager.onCleaned(level, h.bed, c.getSource().getPlayer(), h.remaining);
        ok(c.getSource(), describe(h));
        return 1;
    }

    /** Count the real dirt units in the house and compare with the tracked counter (T5 check). */
    private static int verify(CommandContext<CommandSourceStack> c) {
        House h = house(c);
        if (h == null) return fail(c.getSource(), "No such house");
        ServerLevel level = c.getSource().getLevel();
        int real = countUnits(level, h.box.inflatedBy(1));
        boolean ok = real == h.remaining;
        ok(c.getSource(), (ok ? "VERIFY OK" : "VERIFY MISMATCH") + " house " + h.id + " tracked=" + h.remaining + " real=" + real
                + " total=" + h.total + " phase=" + h.phase);
        return ok ? 1 : 0;
    }

    public static int countUnits(ServerLevel level, BoundingBox b) {
        int real = 0;
        for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof Cleanable cl) real += cl.units(s);
        }
        return real;
    }

    /** Scrub N dirt units like a player would (without a player): exercises the counter and the phase changes. */
    private static int scrub(CommandContext<CommandSourceStack> c) {
        House h = house(c);
        if (h == null) return fail(c.getSource(), "No such house");
        ServerLevel level = c.getSource().getLevel();
        int want = IntegerArgumentType.getInteger(c, "units");
        int done = 0;
        BoundingBox b = h.box.inflatedBy(1);
        outer:
        for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
            while (done < want) {
                BlockState s = level.getBlockState(p);
                if (!(s.getBlock() instanceof Cleanable cl)) continue outer;
                BlockState next = cl.cleanedOnce(s);
                final BlockPos fp = p.immutable();
                Cleaning.suppressLoss(() -> level.setBlock(fp, next, Block.UPDATE_ALL));
                done++;
                HouseManager.onCleaned(level, fp, c.getSource().getPlayer(), 1);
            }
            break;
        }
        ok(c.getSource(), "Scrubbed " + done + " -> " + describe(h));
        return done;
    }

    /** Find the nearest village, load it and make its houses dirty right now (ignores chance/new-village rules). */
    private static int village(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        BlockPos from = BlockPos.containing(c.getSource().getPosition());
        BlockPos found = level.findNearestMapStructure(net.minecraft.tags.StructureTags.VILLAGE, from, 100, false);
        if (found == null) return fail(c.getSource(), "No village found");
        int cx = found.getX() >> 4, cz = found.getZ() >> 4;
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) level.getChunk(cx + dx, cz + dz);
        int made = HouseManager.forceVillage(level, found);
        ok(c.getSource(), "VILLAGE at " + found.toShortString() + " biome=" + level.getBiome(found).getRegisteredName() + " dirty houses created: " + made);
        for (House h : HouseData.get(level).houses()) ok(c.getSource(), describe(h));
        return made;
    }

    private static int remove(CommandContext<CommandSourceStack> c) {
        House h = house(c);
        if (h == null) return fail(c.getSource(), "No such house");
        HouseManager.forcePhase(c.getSource().getLevel(), h, House.Phase.DONE);
        HouseData.get(c.getSource().getLevel()).remove(h.id);
        ok(c.getSource(), "Removed house " + h.id);
        return 1;
    }

    /** Make the village house you are standing in dirty. */
    private static int here(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        BlockPos pos = BlockPos.containing(c.getSource().getPosition());
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, net.minecraft.tags.StructureTags.VILLAGE);
        if (start == null || !start.isValid()) return fail(c.getSource(), "Not inside a village piece");
        for (StructurePiece piece : start.getPieces()) {
            if (piece.getBoundingBox().isInside(pos)) {
                House h = HouseManager.createHouse(level, HouseData.get(level), piece.getBoundingBox(), RandomSource.create(), "plains");
                if (h == null) return fail(c.getSource(), "This piece has no bed or is too small");
                ok(c.getSource(), "Created " + describe(h));
                return 1;
            }
        }
        return fail(c.getSource(), "No piece here");
    }

    /** Build a small oak test house in front of the player (or at the command position) and make it dirty. */
    private static int testHouse(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        ServerPlayer player = c.getSource().getPlayer();
        Direction dir = player != null ? player.getDirection() : Direction.NORTH;
        BlockPos at = BlockPos.containing(c.getSource().getPosition());
        if (player == null) at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
        BlockPos base = at.relative(dir, 3).relative(dir.getCounterClockWise(), 4);
        BoundingBox box = buildTestHouse(level, base, dir);
        House h = HouseManager.createHouse(level, HouseData.get(level), box, RandomSource.create(), "plains");
        if (h == null) return fail(c.getSource(), "Test house could not be made dirty");
        ok(c.getSource(), "Created " + describe(h));
        return 1;
    }

    /** 9x9 one-room oak house. The door faces back towards the player. */
    public static BoundingBox buildTestHouse(ServerLevel level, BlockPos origin, Direction forward) {
        Direction right = forward.getClockWise();
        int y0 = origin.getY();
        java.util.function.BiFunction<Integer, Integer, BlockPos> at = (a, b) -> origin.relative(right, a).relative(forward, b);
        // clear area
        for (int a = -2; a <= 10; a++) for (int b = -3; b <= 10; b++) for (int y = 0; y <= 7; y++) {
            level.setBlock(at.apply(a, b).atY(y0 + y), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (int a = -2; a <= 10; a++) for (int b = -3; b <= 10; b++) {
            level.setBlock(at.apply(a, b).atY(y0 - 1), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(at.apply(a, b).atY(y0), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (int a = 0; a <= 8; a++) for (int b = 0; b <= 8; b++) {
            BlockPos p = at.apply(a, b);
            boolean edge = a == 0 || a == 8 || b == 0 || b == 8;
            boolean corner = (a == 0 || a == 8) && (b == 0 || b == 8);
            level.setBlock(p.atY(y0), edge ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int y = 1; y <= 4; y++) {
                if (!edge) continue;
                BlockState s = corner ? Blocks.OAK_LOG.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
                boolean window = (y == 2 || y == 3) && !corner && ((a == 2 || a == 6) || (b == 2 || b == 6));
                if (window) s = Blocks.GLASS_PANE.defaultBlockState();
                level.setBlock(p.atY(y0 + y), s, Block.UPDATE_ALL);
            }
            level.setBlock(p.atY(y0 + 5), (edge ? Blocks.SPRUCE_PLANKS : Blocks.OAK_PLANKS).defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        // door in the middle of the side facing the player (b == 0)
        BlockPos door = at.apply(4, 0).atY(y0 + 1);
        Direction out = forward.getOpposite();
        level.setBlock(door, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, out).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS);
        level.setBlock(door.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, out).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        // bed in the back corner (foot towards the room)
        BlockPos foot = at.apply(6, 6).atY(y0 + 1);
        BlockPos head = foot.relative(forward);
        level.setBlock(foot, Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState().setValue(BedBlock.FACING, forward).setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
        level.setBlock(head, Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState().setValue(BedBlock.FACING, forward).setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
        level.setBlock(at.apply(1, 7).atY(y0 + 1), Blocks.CRAFTING_TABLE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(at.apply(1, 1).atY(y0 + 1), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(at.apply(7, 1).atY(y0 + 1), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(at.apply(4, 7).atY(y0 + 3), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, out), Block.UPDATE_ALL);
        BlockPos a = at.apply(-1, -1), b = at.apply(9, 9);
        return new BoundingBox(Math.min(a.getX(), b.getX()), y0, Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), y0 + 6, Math.max(a.getZ(), b.getZ()));
    }
}
