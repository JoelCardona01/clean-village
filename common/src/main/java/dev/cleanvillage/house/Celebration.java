package dev.cleanvillage.house;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.cleanvillage.CleanVillage;
import dev.cleanvillage.Config;
import dev.cleanvillage.registry.ModContent;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The final sequence (C1-C4, R1-R4): the house shines, the owner walks in, looks around,
 * jumps with hearts while a jingle plays, then throws a letter and a reward to every helper.
 */
public final class Celebration {
    private Celebration() {}

    public static final ResourceKey<LootTable> REWARD_TABLE =
            ResourceKey.create(Registries.LOOT_TABLE, CleanVillage.id("gameplay/house_reward"));

    private static final int LOOK_TICKS = 30;
    private static final int JUMP_EVERY = 13;
    private static final int JUMPS = 4;

    static void tick(ServerLevel level, HouseData data, House h, Villager v) {
        h.timer++;
        v.setPermanentlyInvulnerable(true); // never interrupted (C4)
        if (v.isPassenger()) v.stopRiding();
        if (v.isNoAi()) v.setNoAi(false);
        if (!v.entityTags().contains(HouseManager.SCRIPTED_TAG)) v.addTag(HouseManager.SCRIPTED_TAG);
        Vec3 inside = Vec3.atBottomCenterOf(h.insidePos);
        switch (h.step) {
            case 0 -> {
                flash(level, h);
                h.step = 1;
                h.timer = 0;
            }
            case 1 -> {
                if (h.timer == 10) {
                    Doors.open(level, h.box, true);
                    v.getNavigation().moveTo(inside.x, inside.y, inside.z, 0.6);
                }
                if (h.timer > 10) {
                    double dist = v.position().distanceTo(inside);
                    if (dist < 1.3) {
                        next(level, h, v);
                    } else if (h.timer > 10 + 120) {
                        CleanVillage.LOG.info("House {}: owner could not walk in (at {}), teleporting", h.id, v.blockPosition());
                        poofTeleport(level, v, inside); // plan B
                        next(level, h, v);
                    } else if (h.timer % 20 == 0 || v.getNavigation().isDone()) {
                        v.getNavigation().moveTo(inside.x, inside.y, inside.z, 0.6);
                    }
                }
            }
            case 2 -> {
                // look around the shiny house
                double ang = Math.toRadians(h.timer * 13.0);
                v.getLookControl().setLookAt(v.getX() + Math.sin(ang) * 3, v.getEyeY() + 0.4 * Math.sin(h.timer * 0.4), v.getZ() + Math.cos(ang) * 3);
                if (h.timer % 8 == 0) {
                    level.sendParticles(ParticleTypes.WAX_OFF, v.getX(), v.getY() + 1.2, v.getZ(), 4, 1.2, 0.8, 1.2, 0.3);
                }
                if (h.timer >= LOOK_TICKS) {
                    h.step = 3;
                    h.timer = 0;
                    h.jumps = 0;
                    level.playSound(null, v.blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.2f, 1.0f);
                }
            }
            case 3 -> {
                Melody.tick(level, v.position().add(0, 1, 0), h.timer);
                ServerPlayer near = (ServerPlayer) level.getNearestPlayer(v, 16);
                if (near != null) v.getLookControl().setLookAt(near, 40f, 40f);
                if (h.jumps < JUMPS && h.timer % JUMP_EVERY == 1 && v.onGround()) {
                    v.getJumpControl().jump();
                    h.jumps++;
                    level.sendParticles(ParticleTypes.HEART, v.getX(), v.getY() + 2.1, v.getZ(), 3, 0.4, 0.2, 0.4, 0.1);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 1.0, v.getZ(), 10, 0.5, 0.6, 0.5, 0.1);
                    if (h.jumps % 2 == 0) {
                        level.playSound(null, v.blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0f, 1.1f);
                    }
                }
                if (h.timer % 6 == 0) {
                    level.sendParticles(ParticleTypes.HEART, v.getX(), v.getY() + 2.0, v.getZ(), 1, 0.5, 0.3, 0.5, 0.0);
                }
                if (h.timer >= JUMP_EVERY * JUMPS + 6) {
                    h.step = 4;
                    h.walkTicks = h.timer; // keep the melody clock running
                    h.timer = 0;
                }
            }
            case 4 -> {
                Melody.tick(level, v.position().add(0, 1, 0), h.walkTicks + h.timer);
                if (h.timer == 4) {
                    giveRewards(level, h, v);
                }
                if (h.timer >= Math.max(40, Melody.LENGTH - h.walkTicks + 4)) {
                    finish(level, data, h, v);
                }
            }
            default -> finish(level, data, h, v);
        }
    }

    private static void next(ServerLevel level, House h, Villager v) {
        CleanVillage.LOG.debug("House {}: owner inside at {} (target {})", h.id, v.blockPosition(), h.insidePos);
        v.getNavigation().stop();
        Doors.open(level, h.box, false);
        h.step = 2;
        h.timer = 0;
    }

    /** C1: the whole house shines for an instant; lights come back. */
    private static void flash(ServerLevel level, House h) {
        List<BlockPos> cells = DirtGenerator.interiorCells(level, h.box);
        java.util.Collections.shuffle(cells, new java.util.Random(h.id * 31L + level.getGameTime()));
        int n = Math.min(cells.size(), 160);
        for (int i = 0; i < n; i++) {
            BlockPos p = cells.get(i);
            level.sendParticles(i % 3 == 0 ? ParticleTypes.END_ROD : ParticleTypes.WAX_OFF,
                    p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.4, 0.4, 0.4, 0.02);
        }
        Vec3 c = Vec3.atCenterOf(h.box.getCenter());
        level.playSound(null, c.x, c.y, c.z, ModContent.SPARKLE, SoundSource.BLOCKS, 1.6f, 1.0f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 2.0f, 1.2f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.6f);
        restoreLights(level, h);
    }

    private static void restoreLights(ServerLevel level, House h) {
        for (Map.Entry<Long, String> e : h.removedLights.entrySet()) {
            BlockPos p = BlockPos.of(e.getKey());
            if (!level.getBlockState(p).isAir()) continue;
            try {
                BlockState s = BlockStateParser.parseForBlock(level.holderLookup(Registries.BLOCK), e.getValue(), false).blockState();
                if (s.canSurvive(level, p)) level.setBlock(p, s, Block.UPDATE_ALL);
            } catch (CommandSyntaxException ex) {
                CleanVillage.LOG.warn("Could not restore light {}: {}", e.getValue(), ex.getMessage());
            }
        }
        h.removedLights.clear();
    }

    /** R1-R3: letter + loot + portrait to every helper, Hero of the Village and reputation. */
    private static void giveRewards(ServerLevel level, House h, Villager v) {
        Config cfg = Config.get();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(REWARD_TABLE);
        int rolls = Math.min(cfg.maxLootRolls, 1 + h.total / cfg.unitsPerExtraRoll);
        for (java.util.UUID id : h.helpers) {
            if (!(level.getPlayerByUUID(id) instanceof ServerPlayer player)) continue;
            List<ItemStack> gifts = new ArrayList<>();
            gifts.add(Texts.thankYouLetter(h.story, h.ownerName, player.getName().getString()));
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, v.position())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .create(LootContextParamSets.GIFT);
            for (int i = 0; i < rolls; i++) gifts.addAll(table.getRandomItems(params));
            ItemStack portrait = new ItemStack(ModContent.FAMILY_PORTRAIT_ITEM);
            portrait.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("item.cleanvillage.family_portrait.lore", h.ownerName))));
            gifts.add(portrait);

            boolean close = player.distanceTo(v) < 14;
            Vec3 target = player.position().add(0, 0.5, 0);
            for (ItemStack g : gifts) {
                if (g.isEmpty()) continue;
                if (close) BehaviorUtils.throwItem(v, g, target);
                else if (!player.getInventory().add(g)) player.spawnAtLocation(level, g);
            }
            if (cfg.heroLevel > 0 && cfg.heroMinutes > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, cfg.heroMinutes * 20 * 60,
                        cfg.heroLevel - 1, false, false, true));
            }
            v.getGossips().add(player.getUUID(), GossipType.MAJOR_POSITIVE, 20);
            v.getGossips().add(player.getUUID(), GossipType.MINOR_POSITIVE, 25);
            player.sendSystemMessage(Component.translatable("message.cleanvillage.thanks", h.ownerName));
        }
        level.playSound(null, v.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.2f, 1.0f);
        level.sendParticles(ParticleTypes.HEART, v.getX(), v.getY() + 2.0, v.getZ(), 6, 0.6, 0.3, 0.6, 0.1);
    }

    private static void finish(ServerLevel level, HouseData data, House h, Villager v) {
        h.phase = House.Phase.DONE;
        release(v);
        HouseManager.removeBar(level, h);
        data.setDirty();
        CleanVillage.LOG.info("House #{} of {} is clean", h.id, h.ownerName);
    }

    /** Back to a normal villager life. */
    static void release(Villager v) {
        v.removeTag(HouseManager.SCRIPTED_TAG);
        v.setPermanentlyInvulnerable(false);
        v.setNoAi(false);
        v.setXRot(0);
    }

    static void poofTeleport(ServerLevel level, Villager v, Vec3 to) {
        level.sendParticles(ParticleTypes.POOF, v.getX(), v.getY() + 0.8, v.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        v.getNavigation().stop();
        v.teleportTo(to.x, to.y, to.z);
        level.sendParticles(ParticleTypes.POOF, to.x, to.y + 0.8, to.z, 12, 0.3, 0.5, 0.3, 0.02);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, to.x, to.y + 1.2, to.z, 8, 0.4, 0.5, 0.4, 0.02);
    }
}
