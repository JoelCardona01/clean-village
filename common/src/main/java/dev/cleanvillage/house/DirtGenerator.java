package dev.cleanvillage.house;

import dev.cleanvillage.block.Cleanable;
import dev.cleanvillage.block.DirtyBlock;
import dev.cleanvillage.block.DirtyPaneBlock;
import dev.cleanvillage.block.GrimeBlock;
import dev.cleanvillage.block.MoldBlock;
import dev.cleanvillage.clean.Cleaning;
import dev.cleanvillage.registry.ModContent;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.*;

/**
 * Turns an existing (vanilla) house into a dirty house: grime layers, dirty block variants,
 * fogged windows, mold, cobwebs, no lights, and a chest with the cleaning kit (G1-G3, S1-S3).
 */
public final class DirtGenerator {
    private DirtGenerator() {}

    public static final int MIN_INTERIOR = 20;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    /** Everything the house manager needs to know about the prepared house. */
    public static final class Result {
        public BoundingBox box;
        public int units;
        public BlockPos bedFoot, seat, waitPos, insidePos, chest;
        public float seatYaw;
        public final Map<Long, String> lights = new LinkedHashMap<>();
        public int interior;
    }

    /** Dry run used to decide if a house is suitable (has a bed and enough room). */
    public static boolean suitable(ServerLevel level, BoundingBox box) {
        return findBed(level, box) != null && interiorCells(level, box).size() >= MIN_INTERIOR;
    }

    public static Result apply(ServerLevel level, BoundingBox box, RandomSource rnd) {
        BlockPos bedFoot = findBed(level, box);
        if (bedFoot == null) return null;
        List<BlockPos> interior = interiorCells(level, box);
        if (interior.size() < MIN_INTERIOR) return null;
        Set<BlockPos> interiorSet = new HashSet<>(interior);

        Result r = new Result();
        r.box = box;
        r.bedFoot = bedFoot;
        r.interior = interior.size();

        // --- seat on the edge of the bed, facing into the room
        BlockState bed = level.getBlockState(bedFoot);
        Direction toHead = bed.getValue(AbstractBedBlock.FACING);
        Direction side = null;
        for (Direction d : new Direction[]{toHead.getClockWise(), toHead.getCounterClockWise(), toHead.getOpposite()}) {
            BlockPos p = bedFoot.relative(d);
            if (interiorSet.contains(p) && standable(level, p)) {
                side = d;
                break;
            }
        }
        if (side == null) side = toHead.getClockWise();
        r.seat = bedFoot;
        r.seatYaw = side.toYRot();
        BlockPos seatFront = bedFoot.relative(side);

        // --- door: wait outside, celebrate inside
        r.waitPos = null;
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                Direction f = s.getValue(DoorBlock.FACING);
                for (Direction d : new Direction[]{f, f.getOpposite()}) {
                    BlockPos out = p.relative(d);
                    if (!covered(level, out, box) && standable(level, out)) {
                        BlockPos further = out.relative(d);
                        BlockPos further2 = further.relative(d);
                        if (standable(level, further2) && !covered(level, further2, box)) r.waitPos = further2;
                        else if (standable(level, further)) r.waitPos = further;
                        else r.waitPos = out;
                        break;
                    }
                }
                if (r.waitPos != null) break;
            }
        }
        if (r.waitPos == null) r.waitPos = findOutside(level, box);

        BlockPos center = box.getCenter();
        r.insidePos = interior.stream()
                .filter(p -> standable(level, p) && !p.equals(seatFront))
                .min(Comparator.comparingDouble(p -> p.distSqr(center) + Math.abs(p.getY() - bedFoot.getY()) * 4))
                .orElse(seatFront);

        // --- chest with the cleaning kit next to the bed
        BlockPos head = bedFoot.relative(toHead);
        BlockPos chest = null;
        List<BlockPos> chestCandidates = new ArrayList<>();
        for (BlockPos b : new BlockPos[]{head, bedFoot}) {
            for (Direction d : Direction.Plane.HORIZONTAL) chestCandidates.add(b.relative(d));
        }
        for (BlockPos c : chestCandidates) {
            if (!c.equals(seatFront) && interiorSet.contains(c) && standable(level, c) && !nextToDoor(level, c)) {
                chest = c.immutable();
                break;
            }
        }
        if (chest == null) {
            chest = interior.stream().filter(p -> standable(level, p) && !p.equals(seatFront) && !nextToDoor(level, p))
                    .min(Comparator.comparingDouble(p -> p.distSqr(bedFoot))).orElse(null);
        }
        r.chest = chest;
        final BlockPos chestF = chest;

        Cleaning.suppressLoss(() -> {
            // --- remove lights (dark, gloomy house). Restored after the celebration.
            for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                BlockState s = level.getBlockState(p);
                if ((s.getBlock() instanceof TorchBlock || s.getBlock() instanceof WallTorchBlock || s.getBlock() instanceof LanternBlock)
                        && covered(level, p, box)) {
                    r.lights.put(p.asLong(), BlockStateParser.serialize(s));
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
                    interiorSet.add(p.immutable());
                }
            }
            if (chestF != null) {
                Direction facing = Direction.Plane.HORIZONTAL.stream()
                        .filter(d -> interiorSet.contains(chestF.relative(d)))
                        .findFirst().orElse(Direction.NORTH);
                level.setBlock(chestF, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), Block.UPDATE_ALL);
                interiorSet.remove(chestF);
            }

            int units = 0;
            List<BlockPos> cells = new ArrayList<>(interiorSet);
            cells.sort(Comparator.comparingLong(BlockPos::asLong));
            Collections.shuffle(cells, new Random(rnd.nextLong()));

            // --- dirty variants of the walls, floor and ceiling around the interior
            Set<BlockPos> dirtied = new HashSet<>();
            for (BlockPos c : cells) {
                for (Direction d : Direction.values()) {
                    BlockPos n = c.relative(d);
                    if (!box.isInside(n) || dirtied.contains(n)) continue;
                    double chance = d == Direction.DOWN ? 0.65 : d == Direction.UP ? 0.35 : 0.5;
                    if (rnd.nextDouble() < chance) {
                        int u = makeDirty(level, n, rnd);
                        if (u > 0) {
                            units += u;
                            dirtied.add(n.immutable());
                        }
                    }
                }
            }
            // --- outside: some dirty wall blocks, every window fogged
            for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                BlockState s = level.getBlockState(p);
                if (s.is(Blocks.GLASS_PANE)) {
                    units += makeDirty(level, p, rnd);
                    continue;
                }
                if (dirtied.contains(p) || !ModContent.DIRTY_OF.containsKey(s.getBlock())) continue;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos o = p.relative(d);
                    if (level.getBlockState(o).isAir() && !covered(level, o, box)) {
                        if (rnd.nextDouble() < 0.35) {
                            units += makeDirty(level, p, rnd);
                            dirtied.add(p.immutable());
                        }
                        break;
                    }
                }
            }

            // --- grime, mold and cobwebs in the interior air
            int webs = 0;
            for (BlockPos c : cells) {
                if (!level.getBlockState(c).isAir() || nearFluid(level, c)) continue;
                EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
                for (Direction d : Direction.values()) {
                    BlockPos n = c.relative(d);
                    if (canHoldLayer(level, d, n)) faces.add(d);
                }
                if (faces.isEmpty()) continue;
                int horizontal = 0;
                for (Direction d : Direction.Plane.HORIZONTAL) if (faces.contains(d)) horizontal++;
                boolean ceiling = faces.contains(Direction.UP);
                boolean floor = faces.contains(Direction.DOWN);

                if (ceiling && horizontal >= 1 && !floor && webs < 12 && rnd.nextDouble() < (horizontal >= 2 ? 0.6 : 0.25)) {
                    level.setBlock(c, ModContent.DUSTY_COBWEB.defaultBlockState(), FLAGS);
                    units += 1;
                    webs++;
                    continue;
                }
                if ((horizontal >= 2 || (horizontal >= 1 && ceiling)) && rnd.nextDouble() < 0.3) {
                    EnumSet<Direction> mf = EnumSet.noneOf(Direction.class);
                    for (Direction d : faces) if (d != Direction.DOWN && rnd.nextDouble() < 0.8) mf.add(d);
                    if (!mf.isEmpty()) {
                        int lvl = rnd.nextDouble() < 0.6 ? 2 : 1;
                        level.setBlock(c, faced(ModContent.MOLD.defaultBlockState(), mf).setValue(MoldBlock.LEVEL, lvl), FLAGS);
                        units += lvl;
                        continue;
                    }
                }
                EnumSet<Direction> gf = EnumSet.noneOf(Direction.class);
                for (Direction d : faces) {
                    double chance = d == Direction.DOWN ? 0.6 : d == Direction.UP ? 0.3 : 0.4;
                    if (rnd.nextDouble() < chance) gf.add(d);
                }
                if (!gf.isEmpty()) {
                    int lvl = grimeLevel(rnd);
                    level.setBlock(c, faced(ModContent.GRIME.defaultBlockState(), gf).setValue(GrimeBlock.LEVEL, lvl), FLAGS);
                    units += lvl;
                }
            }
            // --- outside walls: patches of moss/grime so the house looks dirty from the street
            for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                if (!level.getBlockState(p).isAir() || covered(level, p, box) || nearFluid(level, p)) continue;
                EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos n = p.relative(d);
                    if (box.isInside(n) && canHoldLayer(level, d, n)) faces.add(d);
                }
                if (faces.isEmpty() || rnd.nextDouble() > 0.3) continue;
                if (rnd.nextDouble() < 0.45) {
                    level.setBlock(p, faced(ModContent.MOLD.defaultBlockState(), faces).setValue(MoldBlock.LEVEL, 2), FLAGS);
                    units += 2;
                } else {
                    int lvl = grimeLevel(rnd);
                    level.setBlock(p, faced(ModContent.GRIME.defaultBlockState(), faces).setValue(GrimeBlock.LEVEL, lvl), FLAGS);
                    units += lvl;
                }
            }
            r.units = units;
        });
        return r;
    }

    /** A grime/mold layer may only stick to a clean block: never stack it on an already dirty block. */
    private static boolean canHoldLayer(ServerLevel level, Direction d, BlockPos n) {
        BlockState s = level.getBlockState(n);
        return !(s.getBlock() instanceof Cleanable) && MultifaceBlock.canAttachTo(level, d, n, s);
    }

    private static int grimeLevel(RandomSource rnd) {
        double d = rnd.nextDouble();
        return d < 0.2 ? 1 : d < 0.55 ? 2 : 3;
    }

    private static BlockState faced(BlockState s, Set<Direction> faces) {
        for (Direction d : Direction.values()) s = s.setValue(MultifaceBlock.getFaceProperty(d), faces.contains(d));
        return s;
    }

    /** Replace a vanilla block by its dirty variant. Returns dirt units added. */
    public static int makeDirty(ServerLevel level, BlockPos pos, RandomSource rnd) {
        BlockState s = level.getBlockState(pos);
        Block dirty = ModContent.DIRTY_OF.get(s.getBlock());
        if (dirty == null) return 0;
        BlockState d = DirtyPaneBlock.copyProps(s, dirty.defaultBlockState());
        int units = 1;
        if (d.hasProperty(DirtyBlock.STAGE)) {
            units = rnd.nextDouble() < 0.65 ? 2 : 1;
            d = d.setValue(DirtyBlock.STAGE, units);
        }
        level.setBlock(pos, d, FLAGS);
        return units;
    }

    public static BlockPos findBed(ServerLevel level, BoundingBox box) {
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof AbstractBedBlock && s.getValue(AbstractBedBlock.PART) == BedPart.FOOT) return p.immutable();
        }
        return null;
    }

    /** Air cells inside the box that have a roof above them. */
    public static List<BlockPos> interiorCells(ServerLevel level, BoundingBox box) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState s = level.getBlockState(p);
            if ((s.isAir() || s.getBlock() instanceof GrimeBlock || s.getBlock() instanceof MoldBlock) && covered(level, p, box)) {
                out.add(p.immutable());
            }
        }
        return out;
    }

    public static boolean covered(ServerLevel level, BlockPos p, BoundingBox box) {
        BlockPos.MutableBlockPos m = p.mutable();
        for (int y = p.getY() + 1; y <= box.maxY() + 3; y++) {
            m.setY(y);
            BlockState s = level.getBlockState(m);
            if (!s.isAir() && !s.getCollisionShape(level, m).isEmpty()) return true;
        }
        return false;
    }

    public static boolean standable(ServerLevel level, BlockPos p) {
        BlockState below = level.getBlockState(p.below());
        return passable(level, p) && passable(level, p.above()) && below.isFaceSturdy(level, p.below(), Direction.UP);
    }

    private static boolean passable(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.getCollisionShape(level, p).isEmpty() && !s.liquid();
    }

    /** Never put removable dirt next to water/lava: cleaning it later would let the fluid flow. */
    private static boolean nearFluid(ServerLevel level, BlockPos p) {
        for (Direction d : Direction.values()) {
            if (!level.getFluidState(p.relative(d)).isEmpty()) return true;
        }
        return !level.getFluidState(p).isEmpty();
    }

    private static boolean nextToDoor(ServerLevel level, BlockPos p) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(p.relative(d)).getBlock() instanceof DoorBlock) return true;
        }
        return false;
    }

    private static BlockPos findOutside(ServerLevel level, BoundingBox box) {
        BlockPos c = box.getCenter();
        for (int r = 1; r < 6; r++) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int dx = d.getStepX() * (box.getXSpan() / 2 + r), dz = d.getStepZ() * (box.getZSpan() / 2 + r);
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos p = new BlockPos(c.getX() + dx, box.minY() + dy + 1, c.getZ() + dz);
                    if (standable(level, p)) return p;
                }
            }
        }
        return new BlockPos(c.getX(), box.maxY() + 1, c.getZ());
    }
}
