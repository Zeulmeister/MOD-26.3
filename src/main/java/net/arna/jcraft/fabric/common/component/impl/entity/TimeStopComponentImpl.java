package net.arna.jcraft.fabric.common.component.impl.entity;

import net.arna.jcraft.common.util.JValueIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.common.component.impl.entity.CommonTimeStopComponentImpl;
import net.arna.jcraft.fabric.common.component.JComponents;
import net.arna.jcraft.fabric.common.component.entity.TimeStopComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

@Getter
public class TimeStopComponentImpl extends CommonTimeStopComponentImpl implements TimeStopComponent {
    private final Entity entity;

    public TimeStopComponentImpl(Entity entity) {
        super(entity);
        this.entity = entity;
    }

    @Override
    public void sync(Entity entity) {
        JComponents.TIME_STOP.sync(entity);
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
