package net.arna.jcraft.fabric.common.component.impl.entity;

import net.arna.jcraft.common.util.JValueIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import lombok.NonNull;
import net.arna.jcraft.common.component.impl.entity.CommonGrabComponentImpl;
import net.arna.jcraft.fabric.common.component.JComponents;
import net.arna.jcraft.fabric.common.component.entity.GrabComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class GrabComponentImpl extends CommonGrabComponentImpl implements GrabComponent {
    public GrabComponentImpl(Entity grabbed) {
        super(grabbed);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void sync(Entity entity) {
        JComponents.GRAB.sync(entity);
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
