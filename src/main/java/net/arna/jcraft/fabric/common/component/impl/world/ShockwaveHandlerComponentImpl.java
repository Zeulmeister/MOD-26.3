package net.arna.jcraft.fabric.common.component.impl.world;

import net.arna.jcraft.common.util.JValueIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import lombok.NonNull;
import net.arna.jcraft.common.component.impl.world.CommonShockwaveHandlerComponentImpl;
import net.arna.jcraft.fabric.common.component.JComponents;
import net.arna.jcraft.fabric.common.component.world.ShockwaveHandlerComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class ShockwaveHandlerComponentImpl extends CommonShockwaveHandlerComponentImpl implements ShockwaveHandlerComponent {

    private final Level world;

    public ShockwaveHandlerComponentImpl(Level world) {
        super(world);
        this.world = world;
    }


    @Override
    public void sync(Shockwave shockwave) {
        JComponents.SHOCKWAVE_HANDLER.sync(world, (buf, player) -> writeSyncPacket(buf, shockwave));
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
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        super.writeSyncPacket(buf, recipient);
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        super.applySyncPacket(buf);
    }

    @Override
    public void tick() {
        super.tick();
    }
}
