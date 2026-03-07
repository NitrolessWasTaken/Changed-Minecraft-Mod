package net.ltxprogrammer.changed.process;

import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.data.AccessorySlots;
import net.ltxprogrammer.changed.entity.PlayerDataExtension;
import net.ltxprogrammer.changed.init.ChangedDamageSources;
import net.ltxprogrammer.changed.init.ChangedGameRules;
import net.ltxprogrammer.changed.init.ChangedItems;
import net.ltxprogrammer.changed.init.ChangedTags;
import net.ltxprogrammer.changed.util.ItemUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.concurrent.atomic.AtomicInteger;

public class Pale {
    public static void setPaleExposure(Player player, int level) {
        if (player instanceof PlayerDataExtension ext)
            ext.setPaleExposure(level);
    }

    public static int getPaleExposure(Player player) {
        if (player instanceof PlayerDataExtension ext)
            return ext.getPaleExposure();
        return 0;
    }

    public static boolean isCured(Player player) {
        return getPaleExposure(player) < 0;
    }

    public static boolean tryCure(Player player) {
        if (ProcessTransfur.isPlayerTransfurred(player)) {
            setPaleExposure(player, 0);
            return true;
        }

        return false;
    }

    public static boolean isWearingMask(Player player, Item item) {
        ItemStack helmetStack = player.getItemBySlot(EquipmentSlot.HEAD);
        return helmetStack.is(item);
    }

    // public static int THRESHOLD_IMMUNE_MAX = 12000; // How much your immune system can take until Pale begins to take over
    // public static int THRESHOLD_MINIMAL_DAMAGE = 72000; // 1 minecraft day
    // public static int THRESHOLD_SMALL_DAMAGE = 120000; // 2 minecraft days
    // public static int THRESHOLD_LARGE_DAMAGE = 240000; // 2.5 minecraft days
    // public static int THRESHOLD_DEATH = 432000; // 3 minecraft days

    // testing new config stuff

    public static int THRESHOLD_IMMUNE_MAX = Changed.config.server.paleImmuneMax.get();
    public static int THRESHOLD_MINIMAL_DAMAGE = Changed.config.server.paleMinimalDamageThreshold.get() + THRESHOLD_IMMUNE_MAX;
    public static int THRESHOLD_SMALL_DAMAGE = Changed.config.server.paleSmallDamageThreshold.get() + THRESHOLD_MINIMAL_DAMAGE;
    public static int THRESHOLD_LARGE_DAMAGE = Changed.config.server.paleLargeDamageThreshold.get() + THRESHOLD_SMALL_DAMAGE;
    public static int THRESHOLD_DEATH = Changed.config.server.paleDeathThreshold.get() + THRESHOLD_LARGE_DAMAGE;

    public static int INTERVAL_MINIMAL_DAMAGE = Changed.config.server.paleMinimalDamageInterval.get();
    public static int INTERVAL_SMALL_DAMAGE = Changed.config.server.paleSmallDamageInterval.get();
    public static int INTERVAL_LARGE_DAMAGE = Changed.config.server.paleLargeDamageInterval.get();
    public static int INTERVAL_DEATH_DAMAGE = Changed.config.server.paleDeathDamageInterval.get();

    public static void tickPaleExposure(Player player) {
        if (isCured(player))
            return;
        if (!player.level().getGameRules().getBoolean(ChangedGameRules.RULE_DO_PALE))
            return;
        if (player.isCreative() || player.isSpectator())
            return;

        int exposure = getPaleExposure(player);
        final boolean wearingFaceMask = AccessorySlots.isWearing(player, itemStack -> itemStack.is(ChangedItems.FACE_MASK.get()));
        final boolean wearingGasMask = isWearingMask(player, ChangedItems.GAS_MASK.get());

        AtomicInteger localExposure = new AtomicInteger(0);
        player.level().getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(2.5)).forEach(livingEntity -> {
            if (player == livingEntity) return;

            if (livingEntity.getType().is(ChangedTags.EntityTypes.PALE_SMALL_EXPOSURE) && getPaleExposure(player) < 11800)
                localExposure.addAndGet(wearingGasMask || wearingFaceMask ? 0 : 1);
            else if (livingEntity.getType().is(ChangedTags.EntityTypes.PALE_LARGE_EXPOSURE)) {
                if (!wearingFaceMask && !wearingGasMask)
                    localExposure.addAndGet(2);
                else if ((wearingGasMask || wearingFaceMask) && getPaleExposure(player) < 11800) {// gas mask prevents threshold cross
                    localExposure.addAndGet(1);
                }
            }
            else if (livingEntity instanceof Player otherPlayer) {
                int otherExposure = getPaleExposure(otherPlayer);
                if (otherExposure < THRESHOLD_IMMUNE_MAX)
                    return;
                if (otherExposure >= THRESHOLD_MINIMAL_DAMAGE) {
                    if (!wearingFaceMask && !wearingGasMask)
                        localExposure.addAndGet(1);
                    else if ((wearingFaceMask || wearingGasMask) && getPaleExposure(player) < 11800) // gas mask prevents threshold cross
                        localExposure.addAndGet(1);
                }
                if (otherExposure >= THRESHOLD_SMALL_DAMAGE)
                    localExposure.addAndGet(1);
                if (otherExposure >= THRESHOLD_LARGE_DAMAGE)
                    localExposure.addAndGet(1);
            }
        });

        exposure += localExposure.getAcquire();
        if (exposure > 0 && ProcessTransfur.isPlayerTransfurred(player))
            exposure--;
        if (exposure >= THRESHOLD_IMMUNE_MAX)
            exposure++;


        setPaleExposure(player, exposure);

        if (ProcessTransfur.isPlayerTransfurred(player))
            return;

        // VVV effects VVV
        if (exposure >= THRESHOLD_MINIMAL_DAMAGE && exposure < THRESHOLD_SMALL_DAMAGE) {
            if (exposure % INTERVAL_MINIMAL_DAMAGE < 5) { // 1 Heart per 5 minutes
                player.hurt(ChangedDamageSources.PALE.source(player.level().registryAccess()), 2f);
            }
        } else if (exposure >= THRESHOLD_SMALL_DAMAGE && exposure < THRESHOLD_LARGE_DAMAGE) {
            if (exposure % INTERVAL_SMALL_DAMAGE < 5) { // 1 Heart per 2.5 minutes
                player.hurt(ChangedDamageSources.PALE.source(player.level().registryAccess()), 2f);
            }
        } else if (exposure >= THRESHOLD_LARGE_DAMAGE && exposure < THRESHOLD_DEATH) {
            if (exposure % INTERVAL_LARGE_DAMAGE < 5) { // 1 Heart per 1 minute
                player.hurt(ChangedDamageSources.PALE.source(player.level().registryAccess()), 2f);
            }
        } else if (exposure >= THRESHOLD_DEATH) {
            if (exposure % INTERVAL_DEATH_DAMAGE < 5) { // 1 Heart per 10 seconds
                player.hurt(ChangedDamageSources.PALE.source(player.level().registryAccess()), 2f);
            }
        }
    }
}
