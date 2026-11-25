package net.arna.jcraft.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class HotSandBlock extends FallingBlock {
    public HotSandBlock(final Properties settings) {
        super(settings);
    }

    @Override
    public int getDustColor(final BlockState state, final net.minecraft.world.level.BlockGetter reader, final BlockPos pos) {
        return 0xDBD3A0;
    }

    @Override
    public void stepOn(final Level world, final BlockPos pos, final BlockState state, final Entity entity) {
        if (!entity.isSteppingCarefully() && !entity.isInWater() &&
                entity instanceof final LivingEntity livingEntity && !EnchantmentHelper.hasTag(livingEntity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET), net.minecraft.tags.EnchantmentTags.PREVENTS_ICE_MELTING)) {
            if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                entity.hurtServer(serverLevel, serverLevel.damageSources().hotFloor(), 1.0F);
            }
        }
        super.stepOn(world, pos, state, entity);
    }
}