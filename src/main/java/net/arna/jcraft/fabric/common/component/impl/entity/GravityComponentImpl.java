package net.arna.jcraft.fabric.common.component.impl.entity;

import net.arna.jcraft.common.component.impl.entity.CommonGravityComponentImpl;
import net.arna.jcraft.common.util.JValueIO;
import net.arna.jcraft.fabric.common.component.entity.GravityComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class GravityComponentImpl extends CommonGravityComponentImpl implements GravityComponent {
    public GravityComponentImpl(Entity entity) {
        super(entity);
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
    public void tick() {
        super.tick();
    }
}
