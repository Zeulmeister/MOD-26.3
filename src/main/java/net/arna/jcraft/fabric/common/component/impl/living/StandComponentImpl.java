package net.arna.jcraft.fabric.common.component.impl.living;

import net.arna.jcraft.common.util.JValueIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import lombok.NonNull;
import net.arna.jcraft.common.component.impl.living.CommonStandComponentImpl;
import net.arna.jcraft.fabric.common.component.JComponents;
import net.arna.jcraft.fabric.common.component.living.StandComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class StandComponentImpl extends CommonStandComponentImpl implements StandComponent {

    public StandComponentImpl(Entity entity) {
        super(entity);
    }

    @Override
    public void sync(Entity entity) {
        JComponents.STAND.sync(entity);
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

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        StandComponent.super.applySyncPacket(buf);

        super.applySyncPacket(buf);
    }

    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        StandComponent.super.writeSyncPacket(buf, recipient);

        super.writeSyncPacket(buf, recipient);
    }
}
