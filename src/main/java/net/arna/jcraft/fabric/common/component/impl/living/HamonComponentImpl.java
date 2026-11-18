package net.arna.jcraft.fabric.common.component.impl.living;

import net.arna.jcraft.common.util.JValueIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import lombok.NonNull;
import net.arna.jcraft.common.component.impl.living.CommonHamonComponentImpl;
import net.arna.jcraft.fabric.common.component.JComponents;
import net.arna.jcraft.fabric.common.component.living.HamonComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class HamonComponentImpl extends CommonHamonComponentImpl implements HamonComponent {

    public HamonComponentImpl(LivingEntity entity) {
        super(entity);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void sync(Entity entity) {
        JComponents.HAMON.sync(entity);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayer player) {
        return super.shouldSyncWith(player);
    }

    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        super.writeSyncPacket(buf, recipient);
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        super.applySyncPacket(buf);
    }

    @Override
    public void readData(ValueInput in) {
        super.readFromNbt(JValueIO.read(in, "JCraftData"));
    }

    @Override
    public void writeData(ValueOutput out) {
        CompoundTag tag = new CompoundTag();
        super.writeToNbt(tag);
        JValueIO.write(out, "JCraftData", tag);
    }

}
